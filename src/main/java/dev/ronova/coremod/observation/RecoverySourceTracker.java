package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

/**
 * Bounded observations of SavedData or actual copy-caller static records -> NBT copy -> reconstruction.
 * Discovery performs no external write, invokes no custom map/getter/save implementation,
 * and never promotes UUID equality or a reflected field name into deletion authority.
 */
public final class RecoverySourceTracker implements AutoCloseable {
    // Resolve fixed execution types before an observation window starts. Loading them for the
    // first time while approving a plan changes the code inventory and correctly invalidates it.
    private static final Class<?>[] STORAGE_EXECUTION_TYPES={RecoveryStoragePlan.class,RecoverySnapshotPlan.class,
        RecoveryTaskPlan.class,RecoveryWorkDescriptor.class,RecoveryWorkDescriptor.Kind.class,RecoveryWorkResult.class,
        RecoveryWorkResult.Fact.class,RootExecutionFence.Lease.class,StoragePreparationResult.StorageWritePermit.class,
        StorageCommitRequest.class,StorageTransactionTask.class,StorageTransactionTask.State.class,RecoveryStorageGeneration.class,
        EffectClaim.class,EffectClaim.Status.class};
    public record SourceKey(TargetEpoch targetEpoch, String locator, long observedVersion) {}
    public record Witness(SourceKey source, AuthorityStamp authority, long revision, boolean observerVerified) {}
    private static final int SUBJECT_LIMIT = 128, SOURCE_LIMIT = 256;
    private final DominionServerRuntime runtime;
    private volatile Thread observationThread;
    private final CopyProvenanceIndex<SourceKey> copies;
    private final RecoveryTaskObserver taskObserver;
    private final RecoveryTaskDisposition taskDisposition;
    private enum TaskCandidateStage { CLOSED_OR_SHUTDOWN, SOURCE_SCOPE_UNPROVEN, ADAPTER_UNAVAILABLE,
        OBSERVER_UNAVAILABLE, ANCESTRY_UNPROVEN, SOURCE_UNAVAILABLE, WITNESS_UNAVAILABLE,
        WITNESS_EXPIRED_OR_UNVERIFIED, QUALIFICATION_CHANGED, ROOT_PLAN_UNAVAILABLE, READY }
    // Fixed-size counters and stable registration only; diagnostics retain no runtime task or holder.
    private final long[] taskCandidateStages = new long[TaskCandidateStage.values().length];
    private TaskCandidateStage lastTaskCandidateStage;
    private UUID lastTaskCandidateRegistration;
    private long lastTaskWitnessAgeNanos = -1, taskDispositionQueueDeferrals;
    private RecoveryTaskDisposition.Candidate taskCandidateStopped(TaskExecutionGraph.View view, TaskCandidateStage stage) {
        lastTaskCandidateStage=stage;lastTaskCandidateRegistration=view.registration();
        if(taskCandidateStages[stage.ordinal()]<Long.MAX_VALUE)taskCandidateStages[stage.ordinal()]++;
        return null;
    }
    private final LinkedHashMap<TargetEpoch, Watch> watches = new LinkedHashMap<>();
    private final ArrayDeque<TargetEpoch> rotation = new ArrayDeque<>();
    private final LinkedHashMap<SourceKey, Source> sources = new LinkedHashMap<>();
    private final ArrayDeque<SourceKey> sourceRotation = new ArrayDeque<>();
    private final List<StaticScan> staticScans = new ArrayList<>();
    private final Map<SourceKey, RecordWork> recordTasks = new LinkedHashMap<>();
    private record StoragePreparation(StorageEncodingObservation encoding,StorageImageTask task) {}
    private final Map<SourceKey,StoragePreparation> storagePreparations=new LinkedHashMap<>();
    private final Set<StoragePreparationResult> storageResults=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final Set<StoragePreparationResult> storageCommits=new LinkedHashSet<>();
    private final Set<StorageTransactionTask> closingStorageTransactions=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    /** Immutable Native staging request; only a source-owned acknowledged preparation can construct one. */
    public static final class StorageCommitRequest {
        private final String path,fileIdentity,baselineHash,expectedHash;
        private final byte[] encoded;
        private StorageCommitRequest(String path,RecoveryWorkDescriptor description,NbtStorageImage.Candidate candidate) {
            this.path=java.nio.file.Path.of(path).toAbsolutePath().normalize().toString();
            fileIdentity=description.fileIdentity();baselineHash=description.baselineHash();expectedHash=description.expectedHash();
            if(fileIdentity.isEmpty()||description.storageGeneration()==null||!baselineHash.equals(candidate.baselineHash())||!expectedHash.equals(candidate.expectedHash()))
                throw new IllegalArgumentException("storage request differs from acknowledged image");
            encoded=candidate.encoded();
        }
        public String path(){return path;}
        public String fileIdentity(){return fileIdentity;}
        public String baselineHash(){return baselineHash;}
        public String expectedHash(){return expectedHash;}
        public byte[] encoded(){return encoded.clone();}
        public int encodedSize(){return encoded.length;}
    }
    /** Source-owned handoff. A result's bytes alone never authorize a later operation. */
    public final class StoragePreparationResult implements AutoCloseable {
        private final SourceKey key;
        private final StoragePreparation preparation;
        private StorageImageTask.Output output;
        private RecoveryStoragePlan intent;
        private EffectClaim storageClaim;
        private StorageWritePermit activeWrite;
        private StorageCommitRequest commitRequest;
        private StorageTransactionTask transaction;
        private final EnumSet<RecoveryWorkResult.Fact> storageFacts=EnumSet.noneOf(RecoveryWorkResult.Fact.class);
        private boolean closeRequested;
        public final class StorageWritePermit implements AutoCloseable {
            private final Thread owner=Thread.currentThread();
            private final RootExecutionFence.Lease lease;
            private final dev.ronova.coremod.integrity.EnforcementRuntime.Observation proof;
            private boolean released;
            private StorageWritePermit(RootExecutionFence.Lease lease,dev.ronova.coremod.integrity.EnforcementRuntime.Observation proof) {
                this.lease=lease;this.proof=proof;
            }
            public boolean valid() {
                synchronized(RecoverySourceTracker.this) {
                    return Thread.currentThread()==owner&&!released&&current()&&lease.valid(intent)&&proof.stillCurrent();
                }
            }
            public boolean validFor(StorageCommitRequest expected) {
                synchronized(RecoverySourceTracker.this){return expected!=null&&commitRequest==expected&&valid()
                    &&runtime.durableStore().recoveryStorageClaimCurrent(storageClaim,intent);}
            }
            /** Native alone uses this at the final entry; even another valid lease cannot replay it. */
            public boolean beginCommitFor(StorageCommitRequest expected) {
                synchronized(RecoverySourceTracker.this){return validFor(expected)
                    &&runtime.durableStore().enterRecoveryStorageClaim(storageClaim,intent,lease);}
            }
            private boolean completed() {
                return Thread.currentThread()==owner&&!released&&current()&&lease.valid(intent)&&proof.finish();
            }
            @Override public void close() {
                synchronized(RecoverySourceTracker.this) {
                    if(Thread.currentThread()!=owner)throw new IllegalStateException("storage write permit belongs to another thread");
                    if(released)return;
                    try {lease.close();}finally {proof.close();released=true;activeWrite=null;if(closeRequested)releaseOutput();}
                }
            }
        }
        /** Final Root scope for a future Native executor, never granted merely by an ACK query. */
        public StorageWritePermit acquireWrite() {
            synchronized(RecoverySourceTracker.this) {
                if(!current()||intent==null||activeWrite!=null)return null;
                if(intent.descriptor().fileIdentity().isEmpty()
                    ||intent.descriptor().storageGeneration()==null
                    ||!intent.descriptor().fileIdentity().equals(output.value().baseline().fileIdentity()))return null;
                var acknowledged=runtime.durableStore().recoveryWorkResult(intent.effectKey());
                if(acknowledged==null||!acknowledged.descriptor().equals(intent.descriptor())||acknowledged.facts()!=0)return null;
                var source=sources.get(key);var evidence=source.evidence;
                if(source.memoryDisposition==null&&(evidence==null||!evidence.fresh(System.nanoTime())))return null;
                var proof=runtime.enforcement().beginObservation(0x6000);if(proof==null)return null;
                RootExecutionFence.Lease lease=null;
                try {
                    if(!proof.stillCurrent()||!current())return null;
                    // Renew the transient witness scope, not the durable intent or its attempt.
                    // The original descriptor still fixes location, generation and both file images.
                    var refreshed=source.memoryDisposition==null
                        ?runtime.durableStore().approveRecoveryStoragePlan(source.terminal,evidence,intent.descriptor(),true)
                        :runtime.durableStore().approveStorageAfterMemory(source.memoryDisposition,intent.descriptor(),source.disposedCellBound());
                    if(refreshed==null||!refreshed.effectKey().equals(intent.effectKey()))return null;
                    intent=refreshed;
                    lease=source.memoryDisposition==null?runtime.durableStore().acquireRecoveryStorageWrite(intent,evidence,true)
                        :runtime.durableStore().acquireStorageAfterMemory(intent,source.memoryDisposition,source.disposedCellBound());
                    if(lease==null||!lease.valid(intent)||!proof.stillCurrent()||!current())return null;
                    activeWrite=new StorageWritePermit(lease,proof);return activeWrite;
                } finally {if(activeWrite==null){if(lease!=null)lease.close();proof.close();}}
            }
        }
        /** Stage a private OS image after persistent ACK; acquires no Root execution fence. */
        public StorageTransactionTask startTransaction() {
            synchronized(RecoverySourceTracker.this) {
                if(!current()||intent==null)return null;
                if(transaction!=null)return transaction;
                var ack=runtime.durableStore().recoveryWorkResult(intent.effectKey());
                if(ack==null||!ack.descriptor().equals(intent.descriptor())||ack.facts()!=0)return null;
                commitRequest=new StorageCommitRequest(preparation.encoding().atWrite().observedPath(),intent.descriptor(),output.value().candidate());
                transaction=StorageTransactionTask.start(commitRequest,runtime.resources());return transaction;
            }
        }
        /** Commit only the exact privately staged image under a fresh source/Root permit. */
        public StorageTransactionTask.State commitTransaction() {
            if(Thread.holdsLock(RecoverySourceTracker.this))throw new IllegalStateException("storage entry lock must precede tracker authority");
            StorageTransactionTask task;Source source;Object container,expected;
            synchronized(RecoverySourceTracker.this) {
                if(transaction==null)return StorageTransactionTask.State.CLOSED;
                if(!current()||!transaction.done())return transaction.state();
                if(storageClaim==null)storageClaim=runtime.durableStore().claimRecoveryStorage(intent);
                if(storageClaim==null)return transaction.state();
                task=transaction;source=sources.get(key);container=source.directRecord()?source.map.get():null;
                expected=source.memoryDisposition==null?source.record.get():null;
            }
            StorageTransactionTask.State[] outcome={task.state()};
            Runnable execute=()->{
                synchronized(RecoverySourceTracker.this) {
                    if(transaction!=task||sources.get(key)!=source||!current()||container!=null&&source.map.get()!=container)return;
                    try(var permit=acquireWrite()) {
                        if(permit==null)return;
                        outcome[0]=task.commit(permit);
                        if(outcome[0]==StorageTransactionTask.State.COMMITTED&&!permit.completed()) {
                            task.unverifiedCommit();outcome[0]=StorageTransactionTask.State.UNKNOWN;
                        }
                    }
                }
            };
            if(container!=null)AtomicSnapshotRecord.guardSlot(container,key.targetEpoch().subjectKey().subjectId(),expected,execute);
            else execute.run();
            // Root receipt I/O and independent file reads happen after leaving the container lock.
            synchronized(RecoverySourceTracker.this) {
                var result=outcome[0];
                if(result==StorageTransactionTask.State.COMMITTED) {
                    storageFacts.add(RecoveryWorkResult.Fact.SERIALIZED);storageFacts.add(RecoveryWorkResult.Fact.FILE_COMMITTED);
                    flushStorageFacts();if(transaction==task)task.startReadback();
                } else if(result==StorageTransactionTask.State.UNKNOWN) {
                    storageFacts.add(RecoveryWorkResult.Fact.EFFECT_UNKNOWN);flushStorageFacts();
                }
                return task.state();
            }
        }
        private void flushStorageFacts() {
            if(intent==null)return;
            for(var iterator=storageFacts.iterator();iterator.hasNext();) {
                if(!runtime.durableStore().observeRecoveryWork(intent.effectKey(),iterator.next()))break;
                iterator.remove();
            }
        }
        public StorageTransactionTask.State transactionState() {
            synchronized(RecoverySourceTracker.this) {
                if(transaction==null)return StorageTransactionTask.State.CLOSED;
                var state=transaction.state();
                if(state==StorageTransactionTask.State.READBACK_MATCH&&current()) {
                    storageFacts.add(RecoveryWorkResult.Fact.READBACK_VERIFIED);storageFacts.add(RecoveryWorkResult.Fact.PRESERVATION_VERIFIED);
                }
                flushStorageFacts();return state;
            }
        }
        private String intentStatus="NOT_REQUESTED";
        private String closedStatus="CLOSED";
        public String sourceStatus() {
            synchronized(RecoverySourceTracker.this) {
                if(output==null)return closedStatus;
                if(closeRequested)return "CLOSE_PENDING";
                if(Thread.currentThread()!=observationThread)return "WRONG_THREAD";
                return storagePreparationStatus(key,preparation);
            }
        }
        public String intentStatus(){synchronized(RecoverySourceTracker.this){return intentStatus;}}
        private RecoveryStoragePlan rejectIntent(String reason){intentStatus=reason;return null;}
        private StoragePreparationResult(SourceKey key,StoragePreparation preparation,StorageImageTask.Output output) {
            this.key=key;this.preparation=preparation;this.output=output;
        }
        public boolean current() {
            synchronized(RecoverySourceTracker.this) {
                return sourceStatus().equals("CURRENT");
            }
        }
        public StorageImageTask.Prepared value() {
            synchronized(RecoverySourceTracker.this) {
                if(!current())throw new IllegalStateException("storage preparation no longer current");
                return output.value();
            }
        }
        /** Persist a conditional intent only. No file write, execution lease or completion fact is issued here. */
        public RecoveryStoragePlan prepareIntent() {
            synchronized(RecoverySourceTracker.this) {
                if(!current())return rejectIntent("SOURCE_NOT_CURRENT");
                if(intent!=null) {
                    // Query the original ACK before considering a new observation. This returns
                    // historical preparation metadata only; an executor must acquire a fresh lease.
                    var original=runtime.durableStore().recoveryWorkResult(intent.effectKey());
                    if(original!=null&&original.descriptor().equals(intent.descriptor())) {
                        intentStatus="ACKNOWLEDGED";return intent;
                    }
                }
                // Read compatibility metadata before acquiring a fresh witness; first class loading
                // must not repeatedly invalidate an otherwise valid observation at the approval edge.
                String adapter=StorageAdapterFingerprint.value();if(adapter==null)return rejectIntent("ADAPTER_UNAVAILABLE");
                var source=sources.get(key);var evidence=source.evidence;
                if(source.memoryDisposition==null&&(evidence==null||!evidence.fresh(System.nanoTime())))return rejectIntent("WITNESS_UNAVAILABLE");
                var value=output.value();var candidate=value.candidate();
                if(candidate==null||value.baseline().fileIdentity()==null)return rejectIntent("IMAGE_UNAVAILABLE");
                try(var proof=runtime.enforcement().beginObservation(0x6000)) {
                    if(proof==null||!proof.stillCurrent()||!current())return rejectIntent("OBSERVATION_UNAVAILABLE");
                    var description=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,source.location,
                        source.mutationIncarnation,key.observedVersion(),source.memoryDisposition==null?evidence.copyDepth():source.memoryDisposition.source().copyDepth(),adapter,candidate.baselineHash(),candidate.expectedHash(),false,value.baseline().fileIdentity(),
                        new RecoveryStorageGeneration(RecoveryStorageGeneration.WINDOWS_NTFS_TXF,preparation.encoding().atWrite().traceId(),preparation.encoding().atWrite().generation()));
                    var plan=source.memoryDisposition==null?runtime.durableStore().approveRecoveryStoragePlan(source.terminal,evidence,description,true)
                        :runtime.durableStore().approveStorageAfterMemory(source.memoryDisposition,description,source.disposedCellBound());
                    if(plan==null)return rejectIntent("ROOT_PLAN_REJECTED");
                    if(intent!=null&&!intent.equals(plan))return rejectIntent("ORIGINAL_INTENT_CHANGED");
                    if(!proof.finish()||!current())return rejectIntent("OBSERVATION_CHANGED");
                    intent=plan; // Keep the same original intent if persistence acknowledgement is lost.
                    boolean prepared=source.memoryDisposition==null?runtime.durableStore().prepareRecoveryStorage(plan,evidence,true)
                        :runtime.durableStore().prepareStorageAfterMemory(plan,source.memoryDisposition,source.disposedCellBound());
                    if(!prepared)return rejectIntent("PREPARE_NOT_ACKNOWLEDGED");
                    var acknowledged=runtime.durableStore().recoveryWorkResult(plan.effectKey());
                    if(acknowledged==null||!acknowledged.descriptor().equals(description))return rejectIntent("ACK_QUERY_UNAVAILABLE");
                    intentStatus="ACKNOWLEDGED";return plan;
                }
            }
        }
        @Override public void close() {
            synchronized(RecoverySourceTracker.this) {
                closeRequested=true;
                if(activeWrite!=null){if(Thread.currentThread()==activeWrite.owner)activeWrite.close();return;}
                releaseOutput();
            }
        }
        private void releaseOutput() {
                if(transaction!=null) {
                    transaction.close();if(!transaction.resourcesReleased())closingStorageTransactions.add(transaction);
                    transaction=null;commitRequest=null;
                }
                if(output!=null&&Thread.currentThread()==observationThread) {
                    String status=storagePreparationStatus(key,preparation);closedStatus=status.equals("CURRENT")?"CLOSED":status;
                }
                var owned=output;output=null;storageResults.remove(this);storageCommits.remove(this);if(owned!=null)owned.close();
                flushStorageFacts(); // The original Root intent remains unresolved if acknowledgement is unavailable.
                if(storageClaim!=null){runtime.durableStore().releaseRecoveryStorageClaim(storageClaim);storageClaim=null;}
        }
    }
    private final DeferredReceiptBuffer<RecoveryMutationIntent, RecoveryMutationIntent.Observation> mutationReceipts;
    private final RetiredWorldAdmission<ServerLevel> worldAdmission;
    private record RecordWork(RecordMutationTask task, java.util.concurrent.atomic.AtomicBoolean verified,
        DeferredReceiptBuffer<RecoveryMutationIntent, RecoveryMutationIntent.Observation>.Ticket receipt) {}
    private long version, witnessed, verifiedWitnesses, gaps, stale, inspected, derivedCopies, heldOutputs;
    private long staticDiscoveries;
    private long staticVisit;
    private long clearedStaticSnapshots, unverifiedSnapshotWrites;
    private long clearedLoadedSnapshots;
    private long removedStaticRecords, removedLoadedRecords, unverifiedRecordWrites;
    private long recordTasksSubmitted, recordTasksRejected, recordTasksFinished, recordTasksFailed;
    private Map<String,Long> lastReconstructionProof=Map.of();
    private Map<String,Long> lastBootstrapReconstructionProof=Map.of();
    private boolean closed;
    private boolean sourceReviewTurn;
    private int coldCursor;
    private int historyCursor;
    private long historicalStaticBindings;
    private long historicalStaticSeedMismatches;
    private long historicalInspections, historicalBindings, historicalUnavailable;
    private dev.ronova.coremod.policy.StrengthPolicySnapshot policyView;
    private long policyRevision = -1;

    private static final class StaticScan {
        final WeakReference<Class<?>> caller;
        final SubjectQualification qualification;
        final StaticSnapshotLocator.Cursor cursor = new StaticSnapshotLocator.Cursor();
        final Runnable release;
        long visited;
        int historyCursor;
        boolean historyTurn;
        StaticScan(Class<?> caller, SubjectQualification qualification, Runnable release, long visited) {
            this.caller = new WeakReference<>(caller); this.qualification = qualification; this.release = release;
            this.visited = visited;
        }
        void close() { caller.clear(); cursor.clear(); release.run(); }
    }
    private StaticScan staticCursor(Class<?> caller, SubjectQualification qualification) {
        StaticScan matched = null;
        for (var iterator = staticScans.iterator(); iterator.hasNext();) {
            var scan = iterator.next();
            if (scan.caller.get() == null || retired(scan.qualification)) {
                iterator.remove(); scan.close();
            } else if (scan.caller.get() == caller && scan.qualification.equals(qualification)) matched = scan;
        }
        if (matched != null) return matched;
        if (staticScans.size() >= SUBJECT_LIMIT) { gap(qualification.targetEpoch()); return null; }
        Runnable release = reserve(512);
        if (release == null) { gap(qualification.targetEpoch()); return null; }
        var scan = new StaticScan(caller, qualification, release, staticVisit);
        staticScans.add(scan); return scan;
    }
    private void finishStaticCursor(StaticSnapshotLocator.Cursor cursor) {
        for (var iterator = staticScans.iterator(); iterator.hasNext();) {
            var scan = iterator.next();
            if (scan.cursor == cursor) { iterator.remove(); scan.close(); return; }
        }
    }

    private static final class Watch {
        WeakReference<ServerLevel> level;
        final WeakReference<MinecraftServer> server;
        final DiscoveryCursor<ServerLevel> levels = new DiscoveryCursor<>();
        boolean nextLevel, complete;
        final TerminalOperationPlan plan;
        final Runnable release;
        final DiscoveryCursor<Map.Entry<?,?>> containers = new DiscoveryCursor<>();
        WeakReference<SavedData> data = new WeakReference<>(null);
        String storageKey;
        String storageName;
        Field cacheField;
        WeakReference<DimensionDataStorage> storage = new WeakReference<>(null);
        WeakReference<Map<?,?>> cache = new WeakReference<>(null);
        final InstanceFieldCursor fields = new InstanceFieldCursor();
        boolean fieldsComplete;
        WeakReference<Map<?, ?>> recordMap = new WeakReference<>(null);
        WeakReference<Object> record = new WeakReference<>(null);
        Field recordField;
        final InstanceFieldCursor snapshotFields = new InstanceFieldCursor();
        void clearDataFields() { fields.clear(); fieldsComplete = false; }
        void clearRecord() {
            recordMap.clear(); record.clear(); recordField = null;
            snapshotFields.clear();
        }
        Watch(ServerLevel level, TerminalOperationPlan plan, Runnable release) {
            this.level = new WeakReference<>(level); this.server = new WeakReference<>(level.getServer());
            this.plan = plan; this.release = release;
        }
        void close() {
            levels.clear();containers.clear();data.clear();clearDataFields();clearRecord();level.clear();storage.clear();cache.clear();
            release.run();
        }
    }
    private static final class Source {
        final UUID mutationIncarnation = UUID.randomUUID();
        final SavedDataSaveTrace saveTrace;
        StorageEncodingObservation encoding;
        StorageEncodingObservation confirmedEncoding;
        String confirmedWrittenHash;
        final RecordRetrySchedule recordRetry=new RecordRetrySchedule();
        final SourceKey key;
        final RecoverySourceLocation location;
        final TerminalOperationPlan terminal;
        final WeakReference<Object> owner;
        final boolean staticHolder;
        final StaticSnapshotLocator.WeakBinding staticBinding;
        final WeakReference<Map<?, ?>> map;
        final WeakReference<Object> record;
        final WeakReference<CompoundTag> seed;
        final WeakReference<Object> snapshotSlot;
        final WeakReference<ServerLevel> level;
        final WeakReference<MinecraftServer> server;
        final WeakReference<DimensionDataStorage> storage;
        final Field cacheField;
        final String storageName;
        final Field mapField, snapshotField;
        final Runnable release;
        long witnessRevision;
        RecoverySourceEvidence evidence;
        RecoverySourcePlan plan;
        RecoveryMutationIntent memoryDisposition;
        StoragePreparationResult automaticStorage;
        StorageEncodingObservation storageTerminalEncoding;
        EffectKey storageClosedKey;
        final RecordRetrySchedule storageRetry=new RecordRetrySchedule();
        String storageProgress="WAITING_MEMORY";
        volatile boolean detached;
        volatile boolean worldRetired;
        Source(SourceKey key, Watch watch, SavedData owner, Map<?, ?> map, Object record, CompoundTag seed,
               Field mapField, Field snapshotField, Object snapshotSlot, RecoverySourceLocation location, Runnable release) {
            staticHolder = false; staticBinding = null;
            saveTrace = owner instanceof SavedDataSaveTracked tracked ? tracked.ronova$saveTrace() : null;
            this.key = key; this.terminal=watch.plan; this.owner = new WeakReference<>(owner); this.map = new WeakReference<>(map);
            this.record = new WeakReference<>(record); this.seed = new WeakReference<>(seed);
            this.snapshotSlot=new WeakReference<>(snapshotSlot);
            ServerLevel observedLevel = watch.level.get();
            this.server=new WeakReference<>(watch.server.get());
            this.level = new WeakReference<>(observedLevel);
            this.storage = new WeakReference<>(observedLevel == null ? null : observedLevel.getDataStorage());
            this.cacheField = watch.cacheField; this.storageName = watch.storageName;
            this.mapField = mapField; this.snapshotField = snapshotField; this.release = release;
            this.location = location;
        }
        Source(SourceKey key, TerminalOperationPlan terminal, ServerLevel level,
               StaticSnapshotLocator.Binding binding, RecoverySourceLocation location, Runnable release) {
            this.key = key; this.terminal = terminal; staticHolder = true; staticBinding = binding.weaken();
            saveTrace = null;
            owner = new WeakReference<>(binding.owner()); map = new WeakReference<>(binding.map());
            record = new WeakReference<>(binding.record()); seed = new WeakReference<>((CompoundTag) binding.snapshot());
            snapshotSlot = new WeakReference<>(binding.slot()); this.level = new WeakReference<>(level);
            server = new WeakReference<>(level.getServer()); storage = new WeakReference<>(null);
            cacheField = null; storageName = null;
            mapField = null; snapshotField = null; this.release = release;
            this.location = location;
        }
        boolean worldBound() {
            if (worldRetired) return false;
            var observedLevel = level.get(); var observedServer = server.get();
            return observedLevel != null && observedServer != null
                && observedLevel.getServer() == observedServer
                && observedServer.getLevel(observedLevel.dimension()) == observedLevel;
        }
        private long storageVersion() {
            Object currentStorage = storage.get();
            return currentStorage instanceof StorageMutationTracked tracked ? tracked.ronova$cacheObservationVersion() : -1;
        }
        boolean directRecord() { return staticHolder ? staticBinding.directRecord() : snapshotField == null; }
        boolean disposedCellBound() {
            return memoryDisposition!=null&&(memoryDisposition.kind().record()?recordBound(true):emptyAtomicSlot());
        }
        RecoveryMutationIntent.Kind recordKind() {
            return staticHolder ? RecoveryMutationIntent.Kind.STATIC_RECORD : RecoveryMutationIntent.Kind.LOADED_RECORD;
        }
        /** No world API calls: read only registered storage/cache fields and exact JDK containers. */
        boolean recordBound(boolean absent) {
            if (!directRecord() || worldRetired) return false;
            if (staticHolder) return absent ? staticBinding.recordAbsent() : staticBinding.stillBound();
            return StorageBindingObservation.read(this::storageVersion, () -> LoadedSnapshotRecordBinding.matches(
                storage.get(), cacheField, storageName, owner.get(), mapField, map.get(), key.targetEpoch().subjectKey().subjectId(),
                record.get(), snapshotSlot.get(), seed.get(), absent)) == StorageBindingObservation.BOUND;
        }
        boolean currentStorageWorld() {
            if (!worldBound()) return false;
            return staticHolder || level.get().getDataStorage() == storage.get();
        }
        boolean stillBound() {
            return bindingObservation() == StorageBindingObservation.BOUND;
        }
        StorageBindingObservation bindingObservation() {
            Object data = owner.get(), container = map.get(), value = record.get(), snapshot = seed.get();
            ServerLevel observedLevel = level.get(); DimensionDataStorage observedStorage = storage.get();
            if (data == null || container == null || value == null || snapshot == null) return StorageBindingObservation.DETACHED;
            if (!worldBound()) return StorageBindingObservation.DETACHED;
            if (staticHolder) {
                var observed = staticBinding.bindingObservation();
                if (observed != StorageBindingObservation.BOUND) return observed;
                try {
                    return matchesSeedIdentity((CompoundTag) snapshot, key.targetEpoch().subjectKey().subjectId())
                        ? StorageBindingObservation.BOUND : StorageBindingObservation.DETACHED;
                } catch (RuntimeException | LinkageError unavailable) { return StorageBindingObservation.UNAVAILABLE; }
            }
            return StorageBindingObservation.read(this::storageVersion, () -> {
                if (observedStorage == null || cacheField == null || storageName == null
                    || observedLevel.getDataStorage() != observedStorage) return false;
                Object loaded = cacheField.get(observedStorage);
                boolean currentHolder = safeMap(loaded) && ((Map<?, ?>) loaded).get(storageName) == data
                    && mapField.get(data) == container;
                // Replacing either a loaded SavedData slot or a static map retires the holder binding.
                return currentHolder && safeMap(container)
                    && ((Map<?, ?>) container).get(key.targetEpoch().subjectKey().subjectId()) == value
                    && (snapshotField == null ? value : snapshotField.get(value)) == snapshotSlot.get()
                    && AtomicSnapshotReference.bound(snapshotSlot.get(),snapshot)
                    && matchesSeedIdentity((CompoundTag)snapshot,key.targetEpoch().subjectKey().subjectId());
            });
        }
        boolean emptyAtomicSlot() {
            if (!worldBound()) return false;
            if (staticHolder) return staticBinding.emptyAtomicSlot();
            long observedVersion = storageVersion();
            if (observedVersion < 0) return false;
            var data = owner.get(); var container = map.get(); var value = record.get(); var expectedSlot = snapshotSlot.get();
            var observedLevel = level.get(); var observedStorage = storage.get();
            if (data == null || !safeMap(container) || value == null || expectedSlot == null
                || expectedSlot.getClass() != java.util.concurrent.atomic.AtomicReference.class
                || observedLevel == null || observedLevel.getServer() != server.get()
                || observedLevel.getServer().getLevel(observedLevel.dimension()) != observedLevel
                || observedStorage == null || observedLevel.getDataStorage() != observedStorage
                || cacheField == null || storageName == null) return false;
            try {
                Object loaded = cacheField.get(observedStorage);
                return safeMap(loaded) && ((Map<?,?>) loaded).get(storageName) == data && mapField.get(data) == container
                    && container.get(key.targetEpoch().subjectKey().subjectId()) == value
                    && (snapshotField == null ? value : snapshotField.get(value)) == expectedSlot
                    && ((java.util.concurrent.atomic.AtomicReference<?>) expectedSlot).get() == null
                    && storageVersion() == observedVersion;
            } catch (IllegalAccessException | RuntimeException | LinkageError unavailable) { return false; }
        }
    }
    public RecoverySourceTracker(DominionServerRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP,
            HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "recovery-source-observation/weak-sources-and-leased-cursors-v2");
        runtime.resources().declareAccounting(HostResourceDomain.THREAD,
            HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "record-mutation/worker-permits-until-body-finally");
        copies = new CopyProvenanceIndex<>(2048, this::reserve, this::loss);
        taskObserver = new RecoveryTaskObserver(reserve(512*384+1024*768+2048*192),runtime.enforcement(),this::mergeTaskCopy);
        taskDisposition=new RecoveryTaskDisposition(runtime.durableStore(),runtime.enforcement(),taskObserver.graph(),this::taskCandidate,this::reserve,
            taskObserver::synchronizeCancellationBoundary);
        mutationReceipts = new DeferredReceiptBuffer<>(SOURCE_LIMIT, () -> reserve(4096));
        worldAdmission = new RetiredWorldAdmission<>(SUBJECT_LIMIT, () -> reserve(256));
        RecoveryObservationScope.installHook();
    }
    private Runnable reserve(int bytes) {
        var request = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, (long) bytes),
            HostResourceGovernor.Priority.RECOVERY);
        return request.granted() ? request.reservation()::close : null;
    }
    private boolean current(SubjectQualification q) {
        if (runtime.durableStore() == null) return false;
        var stamp = runtime.durableStore().authorityStamp(q.subjectKey().subjectId());
        return stamp != null && stamp.settled() && !stamp.owner() && q.terminal() && q.equals(stamp.qualification());
    }
    /** An ambiguous write pauses execution; only acknowledged qualification changes retire provenance. */
    private boolean retired(SubjectQualification q) {
        if(runtime.durableStore()==null)return true;
        var stamp=runtime.durableStore().authorityStamp(q.subjectKey().subjectId());
        return stamp==null||stamp.owner()||!q.terminal()||!q.equals(stamp.qualification());
    }
    public synchronized void watch(ServerLevel level, TerminalOperationPlan plan) {
        admit(level,plan,false);
    }
    private boolean admit(ServerLevel level,TerminalOperationPlan plan,boolean cold) {
        if (closed || !level.getServer().isSameThread() || !current(plan.qualification())) return true;
        if (!worldAdmission.allows(level)) { gap(plan.qualification().targetEpoch()); return false; }
        if (!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
            dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return true;
        TargetEpoch key = plan.qualification().targetEpoch();
        Watch existing = watches.get(key);
        if (existing != null) {
            if (current(existing.plan.qualification())) return true;
            watches.remove(key); rotation.remove(key); existing.close();
        }
        // Reserve half of the scan slots for the persistent Root cursor. Repeated
        // live-body executions cannot fill every slot ahead of cold subjects.
        if (watches.size() >= (cold?SUBJECT_LIMIT:SUBJECT_LIMIT/2)) { gap(key); return false; }
        Runnable release = reserve(4096);
        if (release == null) { gap(key); return false; }
        watches.put(key, new Watch(level, plan, release)); rotation.addLast(key);
        return true;
    }
    public synchronized void copied(Object source, Object result, MinecraftServer server) {
        if (closed || server == null || !server.isSameThread()
            || dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(server) != runtime) return;
        taskObserver.bindServerLane();
        var origin = copies.resolve(source);
        if (origin.state() == CopyProvenanceIndex.State.UNOBSERVED && source != null && source.getClass() == CompoundTag.class
            && RecoveryBoundaryProof.copy()) {
            discoverStaticSeed((CompoundTag) source, server, RecoveryBoundaryProof.copyCallers());
            origin = copies.resolve(source);
        }
        if (origin.state() == CopyProvenanceIndex.State.UNOBSERVED) {
            copies.copied(source, result, 1, false); return;
        }
        Source binding=sources.get(origin.sourceKey());
        // A new first edge needs a live source slot. Existing witnessed descendants
        // keep their lineage after a holder replacement, but cannot bless a new seed.
        boolean bound=binding!=null&&current(binding.terminal.qualification())
            &&(origin.copyDepth()>0||!binding.detached&&binding.stillBound());
        boolean derived = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
            dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DERIVED_SOURCE).observationRequested();
        boolean copyVerified=bound&&RecoveryBoundaryProof.copy()&&runtime.enforcement().observesRegisteredSemantics(0x6000);
        if(copyVerified&&origin.copyDepth()==0)trackSeed(binding.key,(CompoundTag)source,true);
        else if(copyVerified)publishCopyOrigin(source);
        copies.copied(source, result, derived ? Integer.MAX_VALUE : 1,copyVerified);
        publishCopyOrigin(result);
        var output=copies.resolve(result);
        if(copyVerified&&output.state()==CopyProvenanceIndex.State.RESOLVED&&output.evidenceVerified()
            &&binding.key.equals(output.sourceKey()))
            taskObserver.synchronousSourceUsed(new TaskExecutionGraph.SourceUse(binding.terminal.qualification(),
                binding.key.locator(),binding.key.observedVersion(),output.copyDepth()));
        if (derived && origin.copyDepth() > 0) derivedCopies = next(derivedCopies);
    }
    private void trackSeed(SourceKey key,CompoundTag seed,boolean verified) {
        copies.track(key,seed,verified);publishCopyOrigin(seed);
    }
    private void publishCopyOrigin(Object value) {
        var resolved=copies.resolve(value);Source source=sources.get(resolved.sourceKey());
        taskObserver.publish(value,resolved.state()==CopyProvenanceIndex.State.RESOLVED&&source!=null
            ?new RecoveryTaskObserver.Origin(source.key,source.terminal.qualification(),resolved.copyDepth(),resolved.evidenceVerified(),System.nanoTime()):null);
    }
    private void mergeTaskCopy(RecoveryTaskObserver.Copy copy) {
        if(closed)return;
        var ticket=copy.origin();
        if(ticket==null) {
            // A captured caller can guide discovery, but cannot retroactively qualify this first copy.
            MinecraftServer server=watches.values().stream().map(watch->watch.server.get()).filter(Objects::nonNull).findFirst().orElse(null);
            if(server!=null&&server.isSameThread()&&copy.source().getClass()==CompoundTag.class)
                discoverStaticSeed((CompoundTag)copy.source(),server,copy.callers());
            return;
        }
        Source source=sources.get(ticket.source());long age=System.nanoTime()-copy.capturedAt();
        var origin=copies.resolve(copy.source());
        boolean current=source!=null&&source.terminal.qualification().equals(ticket.qualification())&&current(ticket.qualification());
        var taskProof=dev.ronova.coremod.nativebridge.NativeBridge.taskObservers();
        boolean verified=current&&copy.verified()&&age>=0&&age<=RecoverySourceEvidence.MAX_AGE_NANOS
            &&taskProof.covers(63)&&taskProof.changes()==copy.taskChanges()
            &&origin.state()==CopyProvenanceIndex.State.RESOLVED&&ticket.source().equals(origin.sourceKey())
            &&origin.evidenceVerified()&&ticket.depth()==origin.copyDepth()&&(origin.copyDepth()>0||!source.detached&&source.stillBound())
            &&runtime.enforcement().observesRegisteredSemantics(0x6000);
        boolean derived=dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
            dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DERIVED_SOURCE).observationRequested();
        copies.copied(copy.source(),copy.result(),derived?Integer.MAX_VALUE:1,verified);publishCopyOrigin(copy.result());
        var output=copies.resolve(copy.result());
        if(verified&&output.state()==CopyProvenanceIndex.State.RESOLVED&&output.evidenceVerified()&&ticket.source().equals(output.sourceKey()))
            taskObserver.graph().sourceUsed(copy.task(),new TaskExecutionGraph.SourceUse(ticket.qualification(),ticket.source().locator(),ticket.source().observedVersion(),output.copyDepth()));
        else if(source!=null)gap(source.key.targetEpoch());
    }
    private RecoveryTaskDisposition.Candidate taskCandidate(TaskExecutionGraph.View view,RecoveryTaskPlan originalPlan) {
        lastTaskWitnessAgeNanos=-1;
        if(closed||runtime.authorityState().shuttingDown())return taskCandidateStopped(view,TaskCandidateStage.CLOSED_OR_SHUTDOWN);
        if(view.sources().size()!=1||view.evidenceLost())return taskCandidateStopped(view,TaskCandidateStage.SOURCE_SCOPE_UNPROVEN);
        Object task=taskObserver.graph().exactTask(view.registration());
        int kind=dev.ronova.coremod.nativebridge.NativeBridge.registeredFutureKind(task);
        if(kind==0)return taskCandidateStopped(view,TaskCandidateStage.ADAPTER_UNAVAILABLE);
        if(!dev.ronova.coremod.nativebridge.NativeBridge.taskObservers().covers(kind==1?1:3))return taskCandidateStopped(view,TaskCandidateStage.OBSERVER_UNAVAILABLE);
        var use=view.sources().iterator().next();
        var ancestry=taskObserver.graph().ancestry(view.registration(),use);if(!ancestry.complete())return taskCandidateStopped(view,TaskCandidateStage.ANCESTRY_UNPROVEN);
        Source source=sources.get(new SourceKey(use.qualification().targetEpoch(),use.locator(),use.sourceVersion()));
        if(source==null)return taskCandidateStopped(view,TaskCandidateStage.SOURCE_UNAVAILABLE);
        long observedNow=System.nanoTime();
        if(source.evidence!=null)lastTaskWitnessAgeNanos=observedNow-source.evidence.observedAtNanos();
        if(!current(use.qualification())||!source.terminal.qualification().equals(use.qualification()))
            return taskCandidateStopped(view,TaskCandidateStage.QUALIFICATION_CHANGED);
        String adapter=dev.ronova.coremod.agent.JdkTaskBoundaryContract.platformImageHash("java/util/concurrent/FutureTask");
        if(kind==2)adapter=EffectRecord.hash(adapter+dev.ronova.coremod.agent.JdkTaskBoundaryContract.platformImageHash("java/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask"));
        var description=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.TASK_CANCEL,source.location,view.registration(),
            source.key.observedVersion(),use.copyDepth(),adapter,EffectRecord.hash("registered-future-state-query"),EffectRecord.hash("cancel-without-runner-interruption"),ancestry.relatedParent());
        var evidence=source.evidence==null?null:new RecoverySourceEvidence(source.evidence.sourceLocator(),source.evidence.authority(),source.evidence.sourceRevision(),
            use.copyDepth(),source.evidence.observerVerified(),source.evidence.observedAtNanos());
        RecoveryTaskPlan plan;
        if(originalPlan!=null&&originalPlan.descriptor().equals(description)
            &&runtime.durableStore().recoveryTaskContinuationCurrent(originalPlan,true))plan=originalPlan;
        else {
            if(evidence==null)return taskCandidateStopped(view,TaskCandidateStage.WITNESS_UNAVAILABLE);
            if(!evidence.fresh(observedNow))return taskCandidateStopped(view,TaskCandidateStage.WITNESS_EXPIRED_OR_UNVERIFIED);
            plan=runtime.durableStore().approveRecoveryTaskPlan(source.terminal,evidence,description,true);
            if(plan==null)return taskCandidateStopped(view,TaskCandidateStage.ROOT_PLAN_UNAVAILABLE);
        }
        java.util.function.BooleanSupplier binding=()->{
            var now=taskObserver.graph().view(view.registration());
            return !closed&&!runtime.authorityState().shuttingDown()&&current(use.qualification())&&taskObserver.graph().exactTask(view.registration())==task
                &&now!=null&&!now.evidenceLost()&&now.sources().equals(view.sources())
                &&taskObserver.graph().ancestry(view.registration(),use).equals(ancestry)
                &&dev.ronova.coremod.nativebridge.NativeBridge.registeredFutureKind(task)==kind;
        };
        taskCandidateStopped(view,TaskCandidateStage.READY);
        return new RecoveryTaskDisposition.Candidate(task,plan,evidence,binding);
    }
    /** The triggering copy is observed synchronously; a later scan cannot retroactively attest this edge. */
    private void discoverStaticSeed(CompoundTag seed, MinecraftServer server, List<Class<?>> callers) {
        if (callers.isEmpty() || runtime.durableStore() == null
            || !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
                dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return;
        try (var observation = runtime.enforcement().beginObservation(0x6000)) {
            if (observation == null || !observation.stillCurrent() || !seed.hasUUID("UUID")) return;
            UUID subject = seed.getUUID("UUID");
            var stamp = runtime.durableStore().authorityStamp(subject);
            var q = stamp == null ? null : stamp.qualification();
            if (q == null || !q.terminal() || !stamp.settled() || stamp.owner() || q.revisions() == null
                || q.revisions().activeIncarnation() == null) return;
            var plan = runtime.durableStore().approveTerminalPlan(subject, q.revisions().activeIncarnation().nonce());
            var level = server.overworld();
            if (plan == null || level == null) return;
            if (!worldAdmission.allows(level)) { gap(q.targetEpoch()); return; }
            WorkBudget budget = runtime.budget(BudgetCategory.OBSERVATION);
            int[] consumed = {0};
            try {
                var historyScan=staticCursor(callers.get(0),q);
                if(historyScan!=null) {
                    historyScan.historyTurn=!historyScan.historyTurn;
                    if(historyScan.historyTurn&&discoverHistoricalStaticSeed(seed,callers,plan,level,budget,consumed,observation,historyScan)) {
                        finishStaticCursor(historyScan.cursor);return;
                    }
                }
                if(!observation.stillCurrent())return;
                var scans = new ArrayList<StaticScan>();
                for (Class<?> caller : callers) {
                    var scan = staticCursor(caller, q);
                    if (scan != null && !scans.contains(scan)) scans.add(scan);
                }
                scans.sort(Comparator.comparingLong(scan -> scan.visited));
                for (var scan : scans) {
                    if (!budget.hasTimeRemaining() || budget.remainingCount() == 0) break;
                    Class<?> caller = scan.caller.get();
                    if (caller == null) { finishStaticCursor(scan.cursor); continue; }
                    scan.visited = staticVisit = next(staticVisit);
                    var cursor = scan.cursor;
                    var found = StaticSnapshotLocator.find(caller, subject, seed, () -> {
                        if (!budget.tryConsumeCount(1)) return false;
                        consumed[0]++; return true;
                    }, cursor);
                    if (found.binding() != null || !found.incomplete()) finishStaticCursor(cursor);
                    if (found.incomplete()) gap(q.targetEpoch());
                    var binding = found.binding();
                    if (binding == null) {
                        if (!budget.hasTimeRemaining() || budget.remainingCount() == 0) break;
                        continue;
                    }
                    if (!binding.stillBound() || !matchesSeedIdentity(seed, subject) || !current(plan.qualification())) return;
                    var location = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER, "", "",
                        binding.owner().getName(), binding.mapField().getName(), binding.snapshotField() == null ? "" : binding.snapshotField().getDeclaringClass().getName(),
                        binding.snapshotField() == null ? "" : binding.snapshotField().getName());
                    String locator = location.locatorHash();
                    for (Source known : sources.values()) {
                        if (known.seed.get() == seed && known.bindingObservation() == StorageBindingObservation.DETACHED) detachSource(known);
                        if (!known.detached && known.staticHolder && known.key.targetEpoch().equals(q.targetEpoch())
                            && known.key.locator().equals(locator) && known.owner.get() == binding.owner()
                            && known.map.get() == binding.map() && known.record.get() == binding.record()
                            && known.seed.get() == seed && known.snapshotSlot.get() == binding.slot()) {
                            trackSeed(known.key, seed, observation.finish()); return;
                        }
                    }
                    if (sources.size() >= SOURCE_LIMIT || version == Long.MAX_VALUE) { gap(q.targetEpoch()); return; }
                    Runnable release = reserve(2048);
                    if (release == null) { gap(q.targetEpoch()); return; }
                    boolean retained = false;
                    try {
                        var key = new SourceKey(q.targetEpoch(), locator, next(version));
                        var discovered = new Source(key, plan, level, binding, location, release);
                        if (!discovered.stillBound() || !current(plan.qualification()) || !observation.finish()) return;
                        if (!worldAdmission.allows(level)) { gap(q.targetEpoch()); return; }
                        version = key.observedVersion(); sources.put(key, discovered); sourceRotation.addLast(key); retained = true;
                        staticDiscoveries = next(staticDiscoveries);
                        trackSeed(key, seed, true);
                    } finally { if (!retained) release.run(); }
                    return;
                }
            } finally { if (consumed[0] > 0) budget.finish(consumed[0], 0); }
        }
    }
    /** Uses only the present copy stack; no retained class name can initialize or select a loader. */
    private boolean discoverHistoricalStaticSeed(CompoundTag seed,List<Class<?>> callers,TerminalOperationPlan plan,
        ServerLevel level,WorkBudget budget,int[] consumed,
        dev.ronova.coremod.integrity.EnforcementRuntime.Observation observation,StaticScan scan) {
        var store=runtime.durableStore();
        if(store==null||store.persistencePending())return false;
        java.util.function.BooleanSupplier allowance=()->{if(!budget.tryConsumeCount(1))return false;consumed[0]++;return true;};
        for(int i=0;i<2&&allowance.getAsBoolean();i++) {
            if(scan.historyCursor>=store.effectRecordCount())scan.historyCursor=0;
            var page=store.effectPage(scan.historyCursor,1);
            if(page.isEmpty())return false;
            scan.historyCursor++;
            var key=page.get(0).key();var location=store.recoveryMutationLocation(key);
            if(location==null||location.scope()!=RecoverySnapshotPlan.StorageScope.STATIC_MANAGER
                ||!key.targetEpoch().equals(plan.qualification().targetEpoch()))continue;
            for(Class<?> caller:callers) {
                var resolved=RecoveryLocationResolver.fromObservedClass(caller,location,plan.subjectKey().subjectId(),allowance);
                if(resolved.state()==RecoveryLocationResolver.State.BUDGET_EXHAUSTED)return false;
                if(resolved.state()!=RecoveryLocationResolver.State.PRESENT)continue;
                if(resolved.view().snapshot()!=seed) {
                    if(observation.stillCurrent())historicalStaticSeedMismatches=next(historicalStaticSeedMismatches);
                    continue;
                }
                var view=resolved.view();
                var binding=new StaticSnapshotLocator.Binding(view.mapField().getDeclaringClass(),view.mapField(),view.container(),
                    plan.subjectKey().subjectId(),view.record(),view.snapshotField(),view.slot(),seed);
                if(!binding.stillBound()||!matchesSeedIdentity(seed,plan.subjectKey().subjectId())
                    ||!current(plan.qualification())||!worldAdmission.allows(level))return false;
                for(Source known:sources.values()) {
                    if(known.seed.get()==seed&&known.bindingObservation()==StorageBindingObservation.DETACHED)detachSource(known);
                    if(!known.detached&&known.staticHolder&&known.key.targetEpoch().equals(key.targetEpoch())
                        &&known.location.equals(location)&&known.owner.get()==binding.owner()&&known.map.get()==binding.map()
                        &&known.record.get()==binding.record()&&known.snapshotSlot.get()==binding.slot()&&known.seed.get()==seed) {
                        boolean verified=known.stillBound()&&observation.finish();
                        trackSeed(known.key,seed,verified);
                        if(verified)historicalStaticBindings=next(historicalStaticBindings);
                        return verified;
                    }
                }
                if(sources.size()>=SOURCE_LIMIT||version==Long.MAX_VALUE){gap(key.targetEpoch());return false;}
                Runnable release=reserve(2048);if(release==null){gap(key.targetEpoch());return false;}
                boolean retained=false;
                try {
                    var sourceKey=new SourceKey(key.targetEpoch(),location.locatorHash(),next(version));
                    var discovered=new Source(sourceKey,plan,level,binding,location,release);
                    if(!discovered.stillBound()||!current(plan.qualification())||!observation.finish()||!worldAdmission.allows(level))return false;
                    version=sourceKey.observedVersion();sources.put(sourceKey,discovered);sourceRotation.addLast(sourceKey);retained=true;
                    staticDiscoveries=next(staticDiscoveries);historicalStaticBindings=next(historicalStaticBindings);
                    trackSeed(sourceKey,seed,true);
                } finally {if(!retained)release.run();}
                return true;
            }
        }
        return false;
    }
    /** Called at the actual registered factory admission seam, never from a UUID search. */
    public synchronized boolean reconstructing(CompoundTag tag, AuthorityStamp proposed) {
        if (closed || tag == null || proposed != null && (proposed.owner() || !proposed.settled())) return false;
        try(var observation=runtime.enforcement().beginObservation(0x6000)) {
            var resolved = copies.resolve(tag);
            if (resolved.state() != CopyProvenanceIndex.State.RESOLVED) return false;
            Source source = sources.get(resolved.sourceKey());
            if (source == null || source.seed.get() != resolved.seed()
                || resolved.copyDepth()==0&&(source.detached||!source.stillBound())) {
                stale = next(stale); return false;
            }
            MinecraftServer server = source.server.get();
            if (server == null || !server.isSameThread() || !current(source.terminal.qualification())) return false;
            AuthorityStamp stamp = runtime.durableStore().authorityStamp(source.key.targetEpoch().subjectKey().subjectId());
            if (stamp == null || !stamp.settled() || stamp.owner() || !source.terminal.qualification().equals(stamp.qualification())) return false;
            // A seed sent directly to a factory also proves use; copy observation is not fabricated.
            source.witnessRevision = next(version); version = source.witnessRevision;
            witnessed = next(witnessed);
            // Historical coverage debt remains in Root, but does not disable a newly
            // proved edge. Unobserved/ambiguous copies cannot pass the resolution checks above.
            boolean boundary=RecoveryBoundaryProof.reconstruction();
            boolean nativeCurrent=observation!=null&&observation.stillCurrent();
            lastReconstructionProof=Map.of("copy_verified",resolved.evidenceVerified()?1L:0L,"boundary_verified",boundary?1L:0L,
                "observation_open",observation!=null?1L:0L,"native_current",nativeCurrent?1L:0L,"depth",(long)resolved.copyDepth());
            if(boundary)lastBootstrapReconstructionProof=lastReconstructionProof;
            boolean verified = resolved.evidenceVerified() && boundary && nativeCurrent;
            var candidate = new RecoverySourceEvidence(source.key.locator(), stamp, source.witnessRevision,
                resolved.copyDepth(), verified, System.nanoTime());
            UUID proposedId = tag.hasUUID("UUID") ? tag.getUUID("UUID") : null;
            // Source identity is checked again immediately before the read-only Root outlet decision.
            boolean held=verified
                &&dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(policy(), resolved.copyDepth(), true).executionPermitted()
                &&(resolved.copyDepth()>0||!source.detached&&source.stillBound())
                &&runtime.durableStore().recoverySourceHeld(source.terminal,candidate,proposedId);
            verified=verified&&observation.finish();
            // Only publish an attestation after the complete read/decision observation survived.
            // A later successful Native cycle cannot turn this attempt's failed proof into a live witness.
            source.evidence=new RecoverySourceEvidence(source.key.locator(),stamp,source.witnessRevision,
                resolved.copyDepth(),verified,candidate.observedAtNanos());
            if(verified)verifiedWitnesses=next(verifiedWitnesses);
            runtime.obligations().reconstruction(new Witness(source.key,stamp,source.witnessRevision,verified));
            if(!held||!verified)return false;
            heldOutputs = next(heldOutputs); return true;
        }
    }
    /** Cold subjects come from the acknowledged projection and are requalified by Root; no body is recreated. */
    public synchronized void resume(MinecraftServer server, WorkBudget budget) {
        if (closed || !server.isSameThread() || runtime.durableStore() == null
            || !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
                dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return;
        var subjects = runtime.projection().snapshot().decisions();
        if (subjects.isEmpty()) { coldCursor = 0; return; }
        int processed = 0;
        try {
            for (int i = 0; i < Math.min(2, subjects.size()) && budget.tryConsumeCount(1); i++) {
                processed++;
                if (coldCursor >= subjects.size()) coldCursor = 0;
                var decision = subjects.get(coldCursor++);
                if (!(decision instanceof dev.ronova.coremod.contract.DominionDecision.Judgment judgment)) continue;
                UUID subject = judgment.subject().uuid();
                var stamp = runtime.durableStore().authorityStamp(subject);
                var q = stamp == null ? null : stamp.qualification();
                if (q == null || !q.terminal() || !stamp.settled() || stamp.owner()
                    || q.revisions() == null || q.revisions().activeIncarnation() == null) continue;
                var plan = runtime.durableStore().approveTerminalPlan(subject, q.revisions().activeIncarnation().nonce());
                var level = server.overworld();
                if (plan != null && level != null && !admit(level,plan,true)) {
                    // Keep this durable subject at the head of the backlog until a
                    // slot opens; a periodic full table must not keep skipping it.
                    coldCursor--;break;
                }
            }
        } finally { if (processed > 0) budget.finish(processed, 0); }
    }
    public synchronized void pulse(WorkBudget budget) {
        if (closed) return;
        collectRecordTasks();
        if ((budget.tickId() & 1) == 0) flushMutationReceipts(budget);
        boolean observe = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
            dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested();
        // Static copies may stop completely after a qualification is retired. Do not
        // rely on another copy callback to release their bounded discovery slots.
        for (var iterator = staticScans.iterator(); iterator.hasNext();) {
            var scan = iterator.next();
            if (!observe || scan.caller.get() == null || retired(scan.qualification)) {
                iterator.remove(); scan.close();
            }
        }
        // Expire borrowed iterators even when this tick has no discovery work allowance.
        // At most SUBJECT_LIMIT handles are visited; inactive scans cannot pin world data indefinitely.
        long now=System.nanoTime();
        for(var watch:watches.values()) {
            if(watch.levels.expire(now)|watch.containers.expire(now)){watch.complete=true;gap(watch.plan.qualification().targetEpoch());}
            var level=watch.level.get();var server=watch.server.get();
            if(server==null||level!=null&&server.getLevel(level.dimension())!=level)watch.complete=true;
            if(watch.complete){watch.levels.clear();watch.containers.clear();}
        }
        int processed = 0, count = Math.min(32, Math.max(1, rotation.size() + sourceRotation.size()) * 4);
        try {
            for (int i = 0; i < count && (!rotation.isEmpty() || !sourceRotation.isEmpty()) && budget.tryConsumeCount(1); i++) {
                processed++; sourceReviewTurn = !sourceReviewTurn;
                if (rotation.isEmpty() || sourceReviewTurn && !sourceRotation.isEmpty()) {
                    var reviewing = sourceRotation.peekFirst();
                    try { reviewSource(); }
                    catch (RuntimeException | LinkageError unavailable) {
                        if (reviewing != null) gap(reviewing.targetEpoch());
                    }
                    continue;
                }
                var key = rotation.removeFirst();
                Watch watch = watches.get(key);
                if (watch == null) { gap(key); continue; }
                if (watch.complete || watch.server.get() == null || retired(watch.plan.qualification()) || !observe) {
                    watches.remove(key); watch.close(); continue;
                }
                rotation.addLast(key);
                if(!current(watch.plan.qualification()))continue;
                try { inspectNext(watch); }
                catch (ReflectiveOperationException | RuntimeException unavailable) {
                    watch.complete=true;gap(watch.plan.qualification().targetEpoch());
                }
                if(watch.complete){watches.remove(key);rotation.remove(key);watch.close();}
            }
        } finally { if (processed > 0) budget.finish(processed, 0); }
        if ((budget.tickId() & 1) != 0) flushMutationReceipts(budget);
    }
    private void flushMutationReceipts(WorkBudget budget) {
        int[] attempts = { 0 };
        try {
            mutationReceipts.flush(4, () -> {
                if (!budget.tryConsumeCount(1)) return false;
                attempts[0]++; return true;
            }, runtime.durableStore()::observeRecoveryMutation);
        } finally { if (attempts[0] > 0) budget.finish(attempts[0], 0); }
    }
    private void inspectNext(Watch watch) throws ReflectiveOperationException {
        MinecraftServer server = watch.server.get();
        if (server == null || !server.isSameThread()) return;
        long now=System.nanoTime();watch.levels.touch(now);watch.containers.touch(now);
        ServerLevel level = watch.level.get();
        if (watch.levels.get(now)==null) {
            watch.levels.set(server.getAllLevels().iterator(),now);
            watch.nextLevel=true;
        }
        if (level == null || watch.nextLevel || server.getLevel(level.dimension()) != level) {
            Iterator<? extends ServerLevel> levels = watch.levels.get(now);
            if (!levels.hasNext()){watch.complete=true;return;}
            level = levels.next();
            if (level.getServer() != server || server.getLevel(level.dimension()) != level) { watch.nextLevel = true; return; }
            watch.level = new WeakReference<>(level); watch.nextLevel = false;
            watch.containers.clear(); watch.data.clear(); watch.clearDataFields(); watch.clearRecord();
            watch.storageName=null;watch.cacheField=null;watch.storage.clear();watch.cache.clear();
        }
        inspected = next(inspected);
        SavedData owner = watch.data.get();
        if(watch.cacheField!=null) {
            var currentStorage=level.getDataStorage();
            Object currentCache=watch.cacheField.get(currentStorage);
            if(!safeMap(currentCache)||currentStorage!=watch.storage.get()||currentCache!=watch.cache.get()) {
                watch.containers.clear();watch.data.clear();watch.clearDataFields();watch.clearRecord();
                watch.cacheField=null;watch.storage.clear();watch.cache.clear();gap(watch.plan.qualification().targetEpoch());return;
            }
            if(owner!=null&&((Map<?,?>)currentCache).get(watch.storageName)!=owner) {
                watch.data.clear();watch.clearDataFields();watch.clearRecord();return;
            }
        }
        // Each snapshot field is a separate budget unit. A record with many reference
        // fields must not multiply one granted inspection into dozens of discoveries.
        if (owner != null && watch.recordField != null) {
            Map<?, ?> map = watch.recordMap.get(); Object record = watch.record.get();
            if (map == null || record == null || watch.recordField.get(owner) != map
                || map.get(watch.plan.subjectKey().subjectId()) != record) {
                watch.clearRecord(); return;
            }
            var next = watch.snapshotFields.next(record.getClass());
            if (next.unavailable()) gap(watch.plan.qualification().targetEpoch());
            if (next.complete()) { watch.clearRecord(); return; }
            Field snapshot = next.field();
            if (snapshot != null) {
                Object value = snapshot.get(record);
                Object image=AtomicSnapshotReference.unwrap(value);
                if (image != null && image.getClass() == CompoundTag.class)
                    remember(watch, watch.recordField, map, record, snapshot, value, (CompoundTag) image);
            }
            return;
        }
        if (owner == null || watch.fieldsComplete) {
            watch.data.clear(); watch.clearDataFields(); watch.clearRecord();
            Iterator<?> containers = watch.containers.get(now);
            if (containers == null) {
                Field cache = storageCache();
                watch.cacheField = cache;
                var storage=level.getDataStorage();Object value = cache.get(storage);
                if (!safeMap(value)) { watch.complete=true;gap(watch.plan.qualification().targetEpoch()); return; }
                watch.storage=new WeakReference<>(storage);watch.cache=new WeakReference<>((Map<?,?>)value);
                // The cursor only borrows a registered map while its world is current.
                var next=((Map<?, ?>) value).entrySet().iterator();
                containers=next;watch.containers.set(next,now);
            }
            if (!containers.hasNext()) { watch.containers.clear(); watch.nextLevel = true; return; }
            Object item = containers.next();
            if (!(item instanceof Map.Entry<?, ?> entry) || !(entry.getKey() instanceof String name)
                || name.length() > 256 || !(entry.getValue() instanceof SavedData data)) return;
            watch.storageKey = level.dimension().location() + "/" + name;
            watch.storageName = name;
            watch.data = new WeakReference<>(data); watch.clearDataFields();
            return;
        }
        var next = watch.fields.next(owner.getClass());
        if (next.unavailable()) gap(watch.plan.qualification().targetEpoch());
        if (next.complete()) { watch.fieldsComplete = true; return; }
        Field field = next.field();
        if (field == null) return;
        Object container = field.get(owner);
        if (!safeMap(container)) return;
        Map<?, ?> map = (Map<?, ?>) container;
        Object record = map.get(watch.plan.subjectKey().subjectId());
        if (record == null) return;
        Object image=AtomicSnapshotReference.unwrap(record);
        if (image!=null&&image.getClass() == CompoundTag.class) {
            remember(watch, field, map, record, null, record, (CompoundTag) image); return;
        }
        if(record.getClass()==java.util.concurrent.atomic.AtomicReference.class)return;
        watch.recordMap = new WeakReference<>(map); watch.record = new WeakReference<>(record);
        watch.recordField = field; watch.snapshotFields.clear();
    }
    private void remember(Watch watch, Field field, Map<?, ?> map, Object record, Field snapshot, Object snapshotSlot, CompoundTag seed) {
        if (!worldAdmission.allows(watch.level.get())) { gap(watch.plan.qualification().targetEpoch()); return; }
        SavedData owner = watch.data.get();
        if (owner == null || version == Long.MAX_VALUE) { gap(watch.plan.qualification().targetEpoch()); return; }
        // A UUID-keyed holder can also contain unrelated entities' snapshots. The
        // original full entity image must identify this subject before copy flow
        // can extend its relation to a replacement UUID at the factory seam.
        if(!matchesSeedIdentity(seed,watch.plan.subjectKey().subjectId()))return;
        var location = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA,
            watch.level.get().dimension().location().toString(), watch.storageName, field.getDeclaringClass().getName(), field.getName(),
            snapshot == null ? "" : snapshot.getDeclaringClass().getName(), snapshot == null ? "" : snapshot.getName());
        String path = location.locatorHash();
        for (Source source : sources.values()) {
            if(source.seed.get()==seed&&source.bindingObservation()==StorageBindingObservation.DETACHED)detachSource(source);
            if (!source.detached && source.key.targetEpoch().equals(watch.plan.qualification().targetEpoch())
                && source.key.locator().equals(path) && source.seed.get() == seed && source.record.get() == record
                && source.owner.get() == owner && source.map.get() == map && source.snapshotSlot.get()==snapshotSlot) {
                if (!source.stillBound()) { gap(source.key.targetEpoch()); return; }
                trackSeed(source.key, seed, runtime.enforcement().observesRegisteredSemantics(0x6000));
                return;
            }
        }
        if (sources.size() >= SOURCE_LIMIT) { gap(watch.plan.qualification().targetEpoch()); return; }
        Runnable release = reserve(2048); // Includes the retained immutable terminal plan after its scan retires.
        if (release == null) { gap(watch.plan.qualification().targetEpoch()); return; }
        version = next(version);
        SourceKey key = new SourceKey(watch.plan.qualification().targetEpoch(), path, version);
        Source source = new Source(key, watch, owner, map, record, seed, field, snapshot, snapshotSlot, location, release);
        if (!worldAdmission.allows(source.level.get()) || !source.stillBound()) {
            try { gap(watch.plan.qualification().targetEpoch()); } finally { release.run(); }
            return;
        }
        sources.put(key, source);
        sourceRotation.addLast(key);
        trackSeed(key, seed, runtime.enforcement().observesRegisteredSemantics(0x6000));
    }
    private void reviewSource() {
        if (sourceRotation.isEmpty()) return;
        SourceKey key = sourceRotation.removeFirst(); Source source = sources.get(key);
        if (source == null) { gap(key.targetEpoch()); return; }
        sourceRotation.addLast(key);
        boolean retired=retired(source.terminal.qualification());
        if (!retired && !source.detached) {
            var binding = source.bindingObservation();
            if (binding == StorageBindingObservation.DETACHED) detachSource(source);
            else if (binding == StorageBindingObservation.UNAVAILABLE) { gap(key.targetEpoch()); return; }
        }
        if(source.detached)copies.retireSeeds(key);
        if (retired || source.detached&&!copies.hasLiveCopies(key)) {
            sources.remove(key); sourceRotation.remove(key); stale = next(stale);
            // Retire the locator before callbacks. A failed copy-accounting release
            // must neither strand the source reservation nor leave an unreviewable source.
            RuntimeException failure=null;
            try { copies.forget(key); } catch(RuntimeException error) { failure=error; }
            try { source.release.run(); } catch(RuntimeException error) {
                if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);
            }
            if(failure!=null)throw failure;
        } else {
            if(source.memoryDisposition!=null) { advanceDisposedStorage(source); return; }
            var evidence = source.evidence;
            if (!current(source.terminal.qualification()) || evidence == null || !evidence.fresh(System.nanoTime())
                || !runtime.enforcement().observesRegisteredSemantics(0x6000)) return;
            if (!queueRecord(source, evidence)) clearSnapshot(source, evidence);
            if (source.plan != null && runtime.durableStore().recoverySourceHeld(source.plan, evidence.copyDepth(), null)) return;
            // Durable I/O happens in the budgeted maintenance lane, never in the factory gate.
            var approved = runtime.durableStore().qualifyRecoverySource(source.terminal, evidence);
            if (approved != null && (!source.detached&&source.stillBound()||source.detached&&evidence.copyDepth()>0)
                && current(source.terminal.qualification())) source.plan = approved;
        }
    }
    /** Queue only direct records in registered concurrent maps. World checks stay on the server thread. */
    private boolean queueRecord(Source source, RecoverySourceEvidence evidence) {
        var existing = recordTasks.get(source.key);
        if (existing != null) return existing.task.active();
        if (recordTasks.size() >= SOURCE_LIMIT) {
            recordTasksRejected = next(recordTasksRejected); gap(source.key.targetEpoch()); return false;
        }
        if (source.detached || !source.directRecord()
            || runtime.authorityState().shuttingDown()) return false;
        Object container = source.map.get(), record = source.record.get();
        var server = source.server.get();
        if (container == null || container.getClass() != ConcurrentHashMap.class || record == null
            || server == null || !server.isSameThread() || !source.stillBound()) return false;
        if(!source.recordRetry.ready(server.getTickCount()))return false;
        var recordPlan = source.staticHolder
            ? runtime.durableStore().approveStaticRecordPlan(source.terminal, evidence, source.key.observedVersion(), true)
            : runtime.durableStore().approveLoadedRecordPlan(source.terminal, evidence, source.key.observedVersion(), true);
        if (recordPlan == null) return false;
        // The acknowledged original intent retains unfinished record/task/save debt.
        // Known queued work is not an observer coverage loss.
        var intent = new RecoveryMutationIntent(recordPlan.source(), source.mutationIncarnation, source.recordKind());
        if (!runtime.durableStore().prepareRecoveryMutation(intent, evidence, source.stillBound(), source.location)) {
            source.recordRetry.failed(server.getTickCount()); return false;
        }
        var receipt = mutationReceipts.reserve(intent);
        if (receipt == null) {
            source.recordRetry.failed(server.getTickCount());
            recordTasksRejected = next(recordTasksRejected); gap(source.key.targetEpoch()); return false;
        }
        var reservation = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, 2048L,
            HostResourceDomain.THREAD, 1L), HostResourceGovernor.Priority.RECOVERY);
        if (!reservation.granted()) {
            receipt.close();
            source.recordRetry.failed(server.getTickCount());
            recordTasksRejected = next(recordTasksRejected); gap(source.key.targetEpoch()); return false;
        }
        Runnable release = reservation.reservation()::close;
        var owner = new WeakReference<>(this);
        var map = source.map; var value = source.record; var key = source.key;
        var verified = new java.util.concurrent.atomic.AtomicBoolean();
        RecordMutationTask task;
        try {
            task = RecordMutationTask.start(active -> {
                Object currentMap = map.get(), currentRecord = value.get();
                return AtomicSnapshotRecord.removeWithPermit(currentMap, key.targetEpoch().subjectKey().subjectId(), currentRecord,
                    () -> {
                        var tracker = owner.get();
                        return tracker == null ? null : tracker.recordPermit(key, intent, active, verified);
                    });
            }, release);
        } catch (RuntimeException | Error failure) {
            source.recordRetry.failed(server.getTickCount());
            recordTasksRejected = next(recordTasksRejected);
            try { release.run(); } finally { receipt.close(); }
            throw failure;
        }
        if (task == null) {
            source.recordRetry.failed(server.getTickCount());
            recordTasksRejected = next(recordTasksRejected);
            try { release.run(); } finally { receipt.close(); }
            gap(key.targetEpoch()); return false;
        }
        recordTasks.put(key, new RecordWork(task, verified, receipt));
        recordTasksSubmitted = next(recordTasksSubmitted);
        return true;
    }
    /** Invoked under the container's lock. Does not call world APIs or foreign record methods. */
    private synchronized AtomicSnapshotRecord.Permit recordPermit(SourceKey key, RecoveryMutationIntent intent, java.util.function.BooleanSupplier active,
        java.util.concurrent.atomic.AtomicBoolean verified) {
        Source source = sources.get(key);
        if (closed || !active.getAsBoolean() || source == null || source.detached
            || runtime.authorityState().shuttingDown() || !source.recordBound(false)
            || !source.mutationIncarnation.equals(intent.incarnation())
            || !runtime.durableStore().recoveryMutationPrepared(intent) || !current(source.terminal.qualification())) return null;
        var observation = runtime.enforcement().beginObservation(0x6000);
        if (observation == null) return null;
        var store = runtime.durableStore();
        dev.ronova.coremod.root.api.RootExecutionFence.Lease acquired = null;
        boolean transferred = false;
        try {
            boolean bound = observation.stillCurrent() && source.recordBound(false);
            var plan = source.staticHolder
                ? store.approveStaticRecordPlan(source.terminal, source.evidence, key.observedVersion(), bound)
                : store.approveLoadedRecordPlan(source.terminal, source.evidence, key.observedVersion(), bound);
            if (plan == null) return null;
            var currentIntent = new RecoveryMutationIntent(plan.source(), source.mutationIncarnation, source.recordKind());
            if (!currentIntent.effectKey().equals(intent.effectKey())) return null;
            acquired = store.acquireRecoveryMutationWrite(currentIntent, source.evidence, source.recordBound(false));
            if (acquired == null) return null;
            var lease = acquired;
            var permit = new AtomicSnapshotRecord.Permit() {
                public boolean valid() {
                    return active.getAsBoolean() && !source.worldRetired && !source.detached
                        && !runtime.authorityState().shuttingDown() && lease.valid(plan)
                        && observation.stillCurrent() && source.recordBound(false)
                        && store.recoveryMutationPrepared(intent);
                }
                public void close() {
                    try { verified.set(source.recordBound(true) && lease.valid(plan) && observation.finish()); }
                    finally { try { observation.close(); } finally { lease.close(); } }
                }
            };
            transferred = true; return permit;
        } finally {
            if (!transferred) { try { observation.close(); } finally { if (acquired != null) acquired.close(); } }
        }
    }
    private void collectRecordTasks() {
        for (var iterator = recordTasks.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next(); var work = entry.getValue(); var source = sources.get(entry.getKey());
            if (source == null || retired(source.terminal.qualification()) || !work.task.active()) work.task.cancel();
            if (!work.task.done()) continue;
            iterator.remove();
            recordTasksFinished = next(recordTasksFinished);
            if (work.task.failed()) recordTasksFailed = next(recordTasksFailed);
            var result = work.task.result();
            var outcome = work.task.observation(false);
            try {
            if(source!=null&&result!=AtomicSnapshotRecord.Result.REMOVED&&result!=AtomicSnapshotRecord.Result.NOT_CURRENT) {
                var server=source.server.get();if(server!=null)source.recordRetry.failed(server.getTickCount());
            }
            if (result == AtomicSnapshotRecord.Result.REMOVED) {
                if (confirmedRecordRemoval(source, work)) {
                    if (source.staticHolder) removedStaticRecords = next(removedStaticRecords);
                    else removedLoadedRecords = next(removedLoadedRecords);
                    outcome = RecoveryMutationIntent.Observation.WRITE_OBSERVED;
                    source.memoryDisposition=work.receipt.key;
                } else {
                    unverifiedRecordWrites = next(unverifiedRecordWrites); outcome = RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
                }
                if (source != null) detachSource(source);
            } else if (result == AtomicSnapshotRecord.Result.NOT_CURRENT) {
                outcome = RecoveryMutationIntent.Observation.NOT_CURRENT;
                if (source != null) detachSource(source);
            } else if (outcome == RecoveryMutationIntent.Observation.WRITE_UNVERIFIED) {
                unverifiedRecordWrites = next(unverifiedRecordWrites); gap(entry.getKey().targetEpoch());
            }
            } catch (RuntimeException | LinkageError unavailable) {
                // A bookkeeping failure cannot erase an already-established operation result.
                if (result == null) outcome = RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
            } finally { work.receipt.complete(outcome); }
        }
    }
    /** Worker evidence is provisional until this server-thread observation confirms its world and holder. */
    private boolean confirmedRecordRemoval(Source source, RecordWork work) {
        if (!work.verified.get() || work.task.failed() || source == null || closed
            || runtime.authorityState().shuttingDown()) return false;
        var server = source.server.get();
        if (server == null || !server.isSameThread()) return false;
        try (var observation = runtime.enforcement().beginObservation(0x6000)) {
            return observation != null && source.currentStorageWorld() && current(source.terminal.qualification())
                && source.recordBound(true) && observation.finish();
        } catch (RuntimeException | LinkageError unavailable) { return false; }
    }
    /** Clears only the observed AtomicReference cell; record/task/persistent effects remain separate. */
    private void clearSnapshot(Source source, RecoverySourceEvidence evidence) {
        if (source.detached || runtime.authorityState().shuttingDown()) return;
        Object slot = source.snapshotSlot.get(); CompoundTag seed = source.seed.get();
        MinecraftServer server = source.server.get();
        if (slot == null || slot.getClass() != java.util.concurrent.atomic.AtomicReference.class || seed == null
            || server == null || !server.isSameThread()) return;
        var store = runtime.durableStore();
        RecoverySnapshotPlan plan;
        try (var observation = runtime.enforcement().beginObservation(0x6000)) {
            if (observation == null || !observation.stillCurrent() || !source.stillBound()) return;
            plan = source.staticHolder
                ? store.approveStaticSnapshotPlan(source.terminal, evidence, source.key.observedVersion(), true)
                : store.approveLoadedSnapshotPlan(source.terminal, evidence, source.key.observedVersion(), true);
            if (plan == null || !observation.finish()) return;
        }
        var intent = new RecoveryMutationIntent(plan, source.mutationIncarnation, source.staticHolder
            ? RecoveryMutationIntent.Kind.STATIC_SNAPSHOT : RecoveryMutationIntent.Kind.LOADED_SNAPSHOT);
        if (!store.prepareRecoveryMutation(intent, evidence, source.stillBound(), source.location)) return;
        var receipt = mutationReceipts.reserve(intent);
        if (receipt == null) { gap(source.key.targetEpoch()); return; }
        var outcome = RecoveryMutationIntent.Observation.NOT_EXECUTED;
        // The durable write can outlive an observation ticket. Observe again instead of extending its lease.
        try (var observation = runtime.enforcement().beginObservation(0x6000)) {
            if (observation == null || !observation.stillCurrent() || !source.stillBound()) return;
            // The acknowledged original intent and reserved receipt retain unfinished
            // task/save debt before the first possible cell write.
            boolean bound = observation.stillCurrent() && source.stillBound();
            try (var lease = store.acquireRecoveryMutationWrite(intent, evidence, bound)) {
                if (lease == null) return;
                outcome = RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
                var result = AtomicSnapshotReference.clear(slot, seed, () -> lease.valid(plan)
                    && !runtime.authorityState().shuttingDown() && observation.stillCurrent()
                    && source.stillBound() && current(source.terminal.qualification()) && store.recoveryMutationPrepared(intent));
                if (result == AtomicSnapshotReference.ClearResult.CLEARED) {
                    boolean verified = source.emptyAtomicSlot() && lease.valid(plan) && observation.finish();
                    outcome = verified ? RecoveryMutationIntent.Observation.WRITE_OBSERVED : RecoveryMutationIntent.Observation.WRITE_UNVERIFIED;
                    if (verified && source.staticHolder) clearedStaticSnapshots = next(clearedStaticSnapshots);
                    else if (verified) clearedLoadedSnapshots = next(clearedLoadedSnapshots);
                    else unverifiedSnapshotWrites = next(unverifiedSnapshotWrites);
                    if(verified)source.memoryDisposition=intent;
                    // This subeffect cannot discharge record/task/save obligations. Keep the checkpoint debt,
                    // and preserve existing copy descendants after the original atomic holder is emptied.
                    detachSource(source);
                } else if (result == AtomicSnapshotReference.ClearResult.NOT_CURRENT) {
                    outcome = RecoveryMutationIntent.Observation.NOT_CURRENT;
                    detachSource(source);
                } else outcome = RecoveryMutationIntent.Observation.NOT_EXECUTED;
            }
        } finally { receipt.complete(outcome); }
    }
    private void detachSource(Source source) {
        // A witnessed own removal is not an unexplained loss of the holder. Keep
        // its task/save obligations and origin until they are independently settled.
        // The original Root write receipt is still required before automatic storage work.
        if(source.memoryDisposition!=null&&!source.worldRetired&&source.disposedCellBound())return;
        if(!source.detached) {
            // Losing a holder is not a disposition receipt. Retain the debt even
            // when all observed copies later disappear; external persistence is unproved.
            source.detached=true;
            try { gap(source.key.targetEpoch()); }
            finally { copies.retireSeeds(source.key); }
            return;
        }
        copies.retireSeeds(source.key);
    }
    /** A registered storage setter invalidates only its observed storage/key bindings. */
    public synchronized void storageReplacing(DimensionDataStorage storage, String name) {
        if (closed || storage == null || name == null) return;
        for (var source : sources.values()) if (!source.staticHolder && source.storage.get() == storage
            && name.equals(source.storageName)) {
            source.worldRetired = true;
            var work = recordTasks.get(source.key);
            if (work != null) work.task.cancel();
        }
        Throwable failure = null;
        for (var source : sources.values()) if (!source.staticHolder && source.storage.get() == storage
            && name.equals(source.storageName)) try { detachSource(source); }
        catch (RuntimeException | LinkageError error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        // Restart only the affected borrowed cache traversal, retaining its target and budget reservation.
        for (var watch : watches.values()) if (watch.storage.get() == storage && name.equals(watch.storageName)) {
            watch.containers.clear(); watch.data.clear(); watch.clearDataFields(); watch.clearRecord();
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof LinkageError error) throw error;
    }
    /** Retire bindings by actual world identity; observed descendants and result receipts keep their own lifetime. */
    public synchronized void worldUnloading(ServerLevel level) {
        if (closed || level == null) return;
        worldAdmission.retire(level);
        // Revoke all matching sources before any accounting or obligation callback can fail.
        for (var source : sources.values()) if (source.level.get() == level) {
            source.worldRetired = true;
            var work = recordTasks.get(source.key);
            if (work != null) work.task.cancel();
        }
        for (var watch : watches.values()) if (watch.level.get() == level) watch.complete = true;
        Throwable failure = null;
        for (var source : sources.values()) if (source.level.get() == level) try { detachSource(source); }
        catch (RuntimeException | LinkageError error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        for (var iterator = watches.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next(); var watch = entry.getValue();
            if (watch.level.get() != level) continue;
            iterator.remove(); rotation.remove(entry.getKey());
            try { gap(entry.getKey()); }
            catch (RuntimeException | LinkageError error) {
                if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
            } finally {
                try { watch.close(); }
                catch (RuntimeException | LinkageError error) {
                    if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
                }
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof LinkageError error) throw error;
    }
    private static boolean safeMap(Object value) {
        return value != null && (value.getClass() == HashMap.class || value.getClass() == LinkedHashMap.class
            || value.getClass() == ConcurrentHashMap.class);
    }
    public synchronized void maintain(MinecraftServer server,WorkBudget budget) {
        if(!closed&&server!=null&&server.isSameThread()){observationThread=Thread.currentThread();taskObserver.bindServerLane();reviewStoragePreparations();}
        if(!closed&&server!=null&&server.isSameThread()&&budget.tryConsumeCount(1)) {
            int allowance=0;
            while(allowance<16&&allowance<taskObserver.queued()&&budget.tryConsumeCount(1))allowance++;
            taskObserver.merge(allowance);
            if(taskObserver.queued()==0)taskDisposition.maintain(budget);
            else if(taskDispositionQueueDeferrals<Long.MAX_VALUE)taskDispositionQueueDeferrals++;
        }
        // Rotate first access to the shared allowance; history must not starve live work or vice versa.
        switch((int)Math.floorMod(budget.tickId(),3)) {
            case 0 -> {resumeHistory(server,budget);resume(server,budget);pulse(budget);}
            case 1 -> {resume(server,budget);pulse(budget);resumeHistory(server,budget);}
            default -> {pulse(budget);resumeHistory(server,budget);resume(server,budget);}
        }
    }
    /** Historical locations are discovery hints. A fresh bound seed still needs actual copy/reconstruction evidence. */
    public synchronized void resumeHistory(MinecraftServer server,WorkBudget budget) {
        var store=runtime.durableStore();
        if(closed||server==null||!server.isSameThread()||store==null||store.readOnly()||store.persistencePending()
            ||!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
                dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested())return;
        int[] consumed={0};
        java.util.function.BooleanSupplier allowance=()->{if(!budget.tryConsumeCount(1))return false;consumed[0]++;return true;};
        try {
            for(int i=0;i<2&&allowance.getAsBoolean();i++) {
                if(historyCursor>=store.effectRecordCount())historyCursor=0;
                var page=store.effectPage(historyCursor,1);
                if(page.isEmpty())return;
                historyCursor++;
                var effect=page.get(0);var location=store.recoveryMutationLocation(effect.key());
                if(location==null||location.scope()!=RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA)continue;
                var subject=effect.key().targetEpoch().subjectKey();
                var stamp=store.authorityStamp(subject.subjectId());
                var qualification=stamp==null?null:stamp.qualification();
                if(stamp==null||!stamp.settled()||stamp.owner()||qualification==null||!qualification.terminal()
                    ||!qualification.targetEpoch().equals(effect.key().targetEpoch())||qualification.revisions()==null
                    ||qualification.revisions().activeIncarnation()==null)continue;
                var plan=store.approveTerminalPlan(subject.subjectId(),qualification.revisions().activeIncarnation().nonce());
                if(plan==null)continue;
                historicalInspections=next(historicalInspections);
                try(var observation=runtime.enforcement().beginObservation(0x6000)) {
                    if(observation==null){historicalUnavailable=next(historicalUnavailable);continue;}
                    var dimension=net.minecraft.resources.ResourceLocation.tryParse(location.dimension());
                    if(dimension==null){historicalUnavailable=next(historicalUnavailable);continue;}
                    var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
                    if(level==null||!worldAdmission.allows(level)){historicalUnavailable=next(historicalUnavailable);continue;}
                    var storage=level.getDataStorage();
                    long sequence=storage instanceof StorageMutationTracked tracked?tracked.ronova$cacheObservationVersion():-1;
                    if(sequence<0){historicalUnavailable=next(historicalUnavailable);continue;}
                    Field cacheField=storageCache();Object rawCache=cacheField.get(storage);
                    if(!safeMap(rawCache)){historicalUnavailable=next(historicalUnavailable);continue;}
                    var cache=(Map<?,?>)rawCache;Object candidate=cache.get(location.storageName());
                    if(!(candidate instanceof SavedData owner)){historicalUnavailable=next(historicalUnavailable);continue;}
                    var result=RecoveryLocationResolver.loaded(owner,location,subject.subjectId(),allowance);
                    if(result.state()!=RecoveryLocationResolver.State.PRESENT){historicalUnavailable=next(historicalUnavailable);continue;}
                    var view=result.view();
                    if(!(view.snapshot() instanceof CompoundTag seed)||seed.getClass()!=CompoundTag.class
                        ||!matchesSeedIdentity(seed,subject.subjectId())||server.getLevel(level.dimension())!=level
                        ||level.getDataStorage()!=storage||cacheField.get(storage)!=cache||cache.get(location.storageName())!=owner
                        ||((StorageMutationTracked)storage).ronova$cacheObservationVersion()!=sequence
                        ||!store.validateTerminalPlan(plan)||!observation.finish()) {
                        historicalUnavailable=next(historicalUnavailable);continue;
                    }
                    // This temporary scan holder owns no cursor/resource and cannot outlive this synchronous read.
                    var watch=new Watch(level,plan,()->{});
                    watch.storageName=location.storageName();watch.storageKey=location.dimension()+"/"+location.storageName();
                    watch.cacheField=cacheField;watch.storage=new WeakReference<>(storage);watch.cache=new WeakReference<>(cache);
                    watch.data=new WeakReference<>(owner);
                    remember(watch,view.mapField(),view.container(),view.record(),view.snapshotField(),view.slot(),seed);
                    if(sources.values().stream().anyMatch(source->!source.detached&&source.key.targetEpoch().equals(effect.key().targetEpoch())
                        &&source.location.equals(location)&&source.seed.get()==seed&&source.owner.get()==owner&&source.stillBound()))
                        historicalBindings=next(historicalBindings);
                } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {
                    historicalUnavailable=next(historicalUnavailable);
                }
            }
        } finally {if(consumed[0]>0)budget.finish(consumed[0],0);}
    }
    public synchronized Map<String,Long> historicalLocationCounters() {
        return Map.of("inspections",historicalInspections,"bound_observations",historicalBindings,"unavailable",historicalUnavailable,
            "static_copy_bindings",historicalStaticBindings,"static_seed_mismatches",historicalStaticSeedMismatches);
    }
    private static boolean matchesSeedIdentity(CompoundTag seed,UUID subject) {
        return seed.getClass()==CompoundTag.class&&seed.hasUUID("UUID")&&subject.equals(seed.getUUID("UUID"));
    }
    private static Field storageCache() throws NoSuchFieldException {
        for (String name : List.of("cache", "f_78144_")) {
            try { Field field = DimensionDataStorage.class.getDeclaredField(name);
                if (field.getType() == Map.class && !Modifier.isStatic(field.getModifiers()) && field.trySetAccessible()) return field;
            } catch (NoSuchFieldException absent) { /* The other mapping may be present. */ }
        }
        throw new NoSuchFieldException("registered DimensionDataStorage cache binding unavailable");
    }
    private void loss(SourceKey key) { gap(key.targetEpoch()); }
    private void gap(TargetEpoch target) { gaps = next(gaps); runtime.obligations().discoveryGap(target); }
    private dev.ronova.coremod.policy.StrengthPolicySnapshot policy() {
        if (runtime.durableStore() == null) return dev.ronova.coremod.policy.StrengthPolicySnapshot.defaults();
        long revision = runtime.durableStore().strengthPolicyConfiguration().revision().value();
        if (policyView == null || revision != policyRevision) {
            policyView = runtime.durableStore().strengthPolicySnapshot(); policyRevision = revision;
        }
        return policyView;
    }
    public synchronized boolean pending(TargetEpoch epoch) {
        return watches.containsKey(epoch) || sources.keySet().stream().anyMatch(key->key.targetEpoch().equals(epoch))
            || staticScans.stream().anyMatch(scan -> scan.qualification.targetEpoch().equals(epoch))
            || mutationReceipts.anyKey(intent -> intent.effectKey().targetEpoch().equals(epoch))
            || recordTasks.keySet().stream().anyMatch(key -> key.targetEpoch().equals(epoch));
    }
    public synchronized Map<String, Long> counters() {
        return Map.of("watched_subjects", (long) watches.size(), "source_records", (long) sources.size(),
            "observed_copies", copies.copyCount(), "reconstruction_observations", witnessed, "coverage_gaps", gaps,
            "stale_sources", stale, "inspection_steps", inspected, "derived_copy_attempts", derivedCopies,
            "verified_witnesses", verifiedWitnesses, "root_held_outputs", heldOutputs);
    }
    /** Advisory save transcripts only: no Root facts, commit receipt, preservation or readback inference. */
    public synchronized Map<SourceKey,SavedDataSaveTrace.Snapshot> storageSaveDiagnostics() {
        var results=new LinkedHashMap<SourceKey,SavedDataSaveTrace.Snapshot>();
        for(var source:sources.values())if(source.saveTrace!=null)results.put(source.key,source.saveTrace.snapshot());
        return Map.copyOf(results);
    }
    public record StorageEncodingObservation(SavedDataSaveTrace.Snapshot atWrite,NbtEncodingPaths.Result positions) {}
    /** Only the owning server thread examines sources; worker saves invalidate old generations without world access here. */
    public void observeStorageEncoding(SavedData owner,CompoundTag image) {
        if(Thread.currentThread()!=observationThread)return;
        synchronized(this) {
            if(closed)return;
            var group=sources.values().stream().filter(source->source.saveTrace!=null&&source.owner.get()==owner).toList();
            if(group.isEmpty())return;
            for(var source:group){source.encoding=null;source.confirmedEncoding=null;source.confirmedWrittenHash=null;}
            int[] remaining={4096};long deadline=System.nanoTime()+2_000_000L;
            int start=group.get(0).saveTrace.nextEncodingStart(group.size());
            for(int index=0;index<group.size()&&remaining[0]>0&&System.nanoTime()<deadline;index++) {
                var source=group.get((start+index)%group.size());
                if(!current(source.terminal.qualification())||!source.stillBound())continue;
                var stamp=source.saveTrace.snapshot();
                if(stamp.active()!=1||stamp.writeAttempts()!=1||stamp.ambiguous())continue;
                var positions=NbtEncodingPaths.find(image,value->{
                    if(value==source.seed.get())return true;
                    var copied=copies.resolve(value);
                    return copied.state()==CopyProvenanceIndex.State.RESOLVED&&copied.evidenceVerified()
                        &&source.key.equals(copied.sourceKey());
                },()->remaining[0]-->0&&System.nanoTime()<deadline);
                if(source.stillBound()&&current(source.terminal.qualification()))
                    source.encoding=new StorageEncodingObservation(stamp,positions);
            }
        }
    }
    /** Advisory positions for this exact successful write generation, not a persistent image/ownership receipt. */
    public void confirmStorageEncoding(SavedData owner,SavedDataSaveTrace.EncodedWrite written) {
        if(Thread.currentThread()!=observationThread)return;
        synchronized(this) {
            if(closed)return;
            long deadline=System.nanoTime()+2_000_000L;
            for(var source:sources.values()) {
                if(System.nanoTime()>=deadline)break;
                if(source.owner.get()!=owner||source.encoding==null||source.saveTrace==null)continue;
                var encoding=source.encoding;var stamp=source.saveTrace.snapshot();
                if(stamp.generation()!=written.generation()||encoding.atWrite().generation()!=written.generation()
                    ||stamp.active()!=1||stamp.failed()||stamp.ambiguous()||!source.stillBound()
                    ||!current(source.terminal.qualification()))continue;
                if(writtenPositionsMatch(source,encoding,written)&&source.stillBound()&&current(source.terminal.qualification())) {
                    source.confirmedEncoding=encoding;source.confirmedWrittenHash=written.hash();
                }
            }
        }
    }
    /** One bounded server-maintenance step. Empty memory alone never enters this state machine. */
    private void advanceDisposedStorage(Source source) {
        var server=source.server.get();if(server==null||!server.isSameThread())return;
        int tick=server.getTickCount();if(!source.storageRetry.ready(tick))return;
        var store=runtime.durableStore();
        if(!current(source.terminal.qualification())||source.detached||!source.currentStorageWorld()||!source.disposedCellBound()) {
            source.storageProgress="BINDING_LOST";if(source.automaticStorage!=null){source.automaticStorage.close();source.automaticStorage=null;}return;
        }
        if(!source.memoryDisposition.acknowledgedWrite(store.recoveryMutationResult(source.memoryDisposition))) {
            source.storageProgress="WAITING_MEMORY_ACK";source.storageRetry.failed(tick);return;
        }
        if(source.storageClosedKey!=null) {
            if(store.observeRecoveryWork(source.storageClosedKey,RecoveryWorkResult.Fact.MEMORY_CHANGED)
                &&store.observeRecoveryWork(source.storageClosedKey,RecoveryWorkResult.Fact.RESOURCES_RELEASED)) {
                source.storageProgress="STORAGE_ACKNOWLEDGED";source.storageClosedKey=null;
            }else {source.storageProgress="WAITING_RESULT_ACK";source.storageRetry.failed(tick);}
            return;
        }
        if(source.encoding!=null&&source.storageTerminalEncoding==source.encoding)return;
        var prepared=source.automaticStorage;
        if(prepared==null) {
            if(source.encoding==null){source.storageProgress="WAITING_OBSERVED_SAVE";return;}
            if(storageResults.stream().anyMatch(value->value.key.equals(source.key))) {
                source.storageProgress="OWNED_BY_CONSUMER";return;
            }
            var pending=storagePreparations.get(source.key);
            if(pending==null) {
                // Conservative defaults within the existing resource governor. Oversized or opaque
                // encodings remain explicit unresolved work, never a successful empty result.
                var task=startStoragePreparation(source.key,65536,262144,4096);
                source.storageProgress=task==null?"PREPARATION_UNAVAILABLE":"PREPARING";
                if(task==null)source.storageRetry.failed(tick);return;
            }
            if(!pending.task().done()){source.storageProgress="PREPARING";return;}
            prepared=takeStoragePreparation(source.key);
            if(prepared==null||prepared.value().candidate()==null) {
                if(prepared!=null)prepared.close();source.storageProgress="IMAGE_UNPROVEN";
                source.storageTerminalEncoding=source.encoding;return;
            }
            source.automaticStorage=prepared;
        }
        if(!prepared.current()) {
            source.storageProgress=prepared.sourceStatus();prepared.close();source.automaticStorage=null;source.storageRetry.failed(tick);return;
        }
        var plan=prepared.prepareIntent();
        if(plan==null){source.storageProgress=prepared.intentStatus();source.storageRetry.failed(tick);return;}
        var transaction=prepared.startTransaction();
        if(transaction==null){source.storageProgress="TRANSACTION_UNAVAILABLE";source.storageRetry.failed(tick);return;}
        var state=prepared.transactionState();source.storageProgress=state.name();
        if(state==StorageTransactionTask.State.READY) {storageCommits.add(prepared);return;}
        if(state==StorageTransactionTask.State.STAGING||state==StorageTransactionTask.State.READING)return;
        if(state==StorageTransactionTask.State.READBACK_MATCH) {
            var result=store.recoveryWorkResult(plan.effectKey());
            if(result==null||!result.has(RecoveryWorkResult.Fact.READBACK_VERIFIED)||!result.has(RecoveryWorkResult.Fact.PRESERVATION_VERIFIED)) {
                source.storageProgress="WAITING_READBACK_ACK";source.storageRetry.failed(tick);return;
            }
            prepared.close();source.automaticStorage=null;
            if(transaction.resourcesReleased()) {
                source.storageClosedKey=plan.effectKey();source.storageTerminalEncoding=source.encoding;
                source.storageProgress="WAITING_RESULT_ACK";
            } else {source.storageProgress="RESOURCE_EXIT_UNPROVEN";source.storageTerminalEncoding=source.encoding;}
            return;
        }
        // No repeated opaque operation on this generation. A new observed save can reopen discovery.
        prepared.close();source.automaticStorage=null;source.storageTerminalEncoding=source.encoding;
    }
    public synchronized Map<SourceKey,String> memoryDispositionDiagnostics() {
        var result=new LinkedHashMap<SourceKey,String>();
        for(var source:sources.values())if(source.memoryDisposition!=null) {
            boolean acknowledged=source.memoryDisposition.acknowledgedWrite(runtime.durableStore().recoveryMutationResult(source.memoryDisposition));
            result.put(source.key,source.detached||!source.disposedCellBound()?"BINDING_LOST":acknowledged?"WRITE_ACKNOWLEDGED":"WRITE_OBSERVED_UNACKNOWLEDGED");
        }
        return Map.copyOf(result);
    }
    public synchronized Map<SourceKey,String> storageProgressDiagnostics() {
        var result=new LinkedHashMap<SourceKey,String>();
        for(var source:sources.values())if(source.memoryDisposition!=null)result.put(source.key,source.storageProgress);
        return Map.copyOf(result);
    }
    /** Run outside the tracker monitor: final entry lock -> tracker -> Root, matching the memory executor. */
    public void drainStorageCommits(MinecraftServer server,WorkBudget budget) {
        if(server==null||!server.isSameThread())return;
        int processed=0;
        try {
            while(processed<2) {
                StoragePreparationResult next;
                synchronized(this) {
                    if(closed||storageCommits.isEmpty()||!budget.tryConsumeCount(1))break;
                    var iterator=storageCommits.iterator();next=iterator.next();iterator.remove();
                }
                processed++;next.commitTransaction();
            }
        } finally {if(processed>0)budget.finish(processed,0);}
    }
    public synchronized Map<SourceKey,StorageEncodingObservation> storageEncodingDiagnostics() {
        var result=new LinkedHashMap<SourceKey,StorageEncodingObservation>();
        for(var source:sources.values())if(source.encoding!=null&&source.saveTrace!=null) {
            var now=source.saveTrace.snapshot();var at=source.encoding.atWrite();
            if(now.traceId().equals(at.traceId())&&now.generation()==at.generation()&&now.active()==0
                &&now.serialized()==1&&now.writeAttempts()==1&&now.writeReturns()==1
                &&now.returned()&&!now.failed()&&!now.ambiguous())result.put(source.key,source.encoding);
        }
        return Map.copyOf(result);
    }
    private boolean storagePreparationCurrent(SourceKey key,StoragePreparation pending) {
        return storagePreparationStatus(key,pending).equals("CURRENT");
    }
    private String storagePreparationStatus(SourceKey key,StoragePreparation pending) {
        var source=sources.get(key);
        if(closed)return "TRACKER_CLOSED";
        if(runtime.authorityState().shuttingDown())return "SHUTDOWN";
        if(source==null)return "SOURCE_RETIRED";
        if(source.encoding!=pending.encoding())return "ENCODING_REPLACED";
        if(!current(source.terminal.qualification()))return "QUALIFICATION_CHANGED";
        if(!source.currentStorageWorld())return "WORLD_CHANGED";
        if(source.memoryDisposition!=null&&!source.memoryDisposition.acknowledgedWrite(
            runtime.durableStore().recoveryMutationResult(source.memoryDisposition)))return "MEMORY_RESULT_UNACKNOWLEDGED";
        var now=source.saveTrace.snapshot();var then=pending.encoding().atWrite();
        if(!now.traceId().equals(then.traceId())||now.generation()!=then.generation())return "SAVE_GENERATION_CHANGED";
        if(now.active()!=0||now.failed()||now.ambiguous())return "SAVE_UNSETTLED";
        var written=source.saveTrace.encodedWrite();
        if(written==null||written.generation()!=then.generation())return "WRITTEN_DIGEST_REVOKED";
        if(written.fileIdentity()==null)return "WRITER_FILE_IDENTITY_UNAVAILABLE";
        if(source.confirmedEncoding!=pending.encoding()||!written.hash().equals(source.confirmedWrittenHash))return "WRITER_POSITIONS_UNCONFIRMED";
        return source.stillBound()||source.recordBound(true)||source.emptyAtomicSlot()?"CURRENT":"BINDING_CHANGED";
    }
    private boolean writtenPositionsMatch(Source source,StorageEncodingObservation encoding,SavedDataSaveTrace.EncodedWrite written) {
        if(!written.references().complete()||!encoding.positions().complete())return false;
        var actual=new java.util.HashSet<java.util.List<NbtStoragePreservation.Step>>();
        for(var entry:written.references().entries()) {
            Object value=entry.value().get();if(value==null)return false;
            if(value==source.seed.get()){actual.add(entry.path());continue;}
            if(value instanceof CompoundTag tag) {
                var copied=copies.resolve(tag);
                if(copied.state()==CopyProvenanceIndex.State.RESOLVED&&copied.evidenceVerified()&&source.key.equals(copied.sourceKey()))
                    actual.add(entry.path());
            }
        }
        return !actual.isEmpty()&&actual.equals(new java.util.HashSet<>(encoding.positions().paths()));
    }
    /** Start read/plan work only; worker receives immutable path/positions and no world references. */
    public synchronized StorageImageTask startStoragePreparation(SourceKey key,int encodedLimit,int decodedLimit,int nodes) {
        if(Thread.currentThread()!=observationThread||closed)return null;
        var existing=storagePreparations.get(key);
        if(existing!=null)return storagePreparationCurrent(key,existing)?existing.task():null;
        if(storagePreparations.size()+storageResults.size()>=2)return null;
        var encoding=storageEncodingDiagnostics().get(key);
        if(encoding==null||!encoding.positions().complete()||encoding.positions().paths().isEmpty()||encoding.atWrite().observedPath()==null)return null;
        var check=new StoragePreparation(encoding,null);if(!storagePreparationCurrent(key,check))return null;
        var written=sources.get(key).saveTrace.encodedWrite();
        if(written==null||written.generation()!=encoding.atWrite().generation())return null;
        var request=new StorageImageTask.Request(java.nio.file.Path.of(encoding.atWrite().observedPath()),encoding.positions().paths(),encodedLimit,decodedLimit,nodes,written.hash(),written.fileIdentity());
        var task=StorageImageTask.start(request,runtime.resources());
        if(task!=null)storagePreparations.put(key,new StoragePreparation(encoding,task));return task;
    }
    /** Revalidate on the server before transferring an owned candidate result to its next stage. */
    public synchronized StoragePreparationResult takeStoragePreparation(SourceKey key) {
        if(Thread.currentThread()!=observationThread)return null;
        var pending=storagePreparations.get(key);if(pending==null||!pending.task().done())return null;
        storagePreparations.remove(key);
        try {
            if(!storagePreparationCurrent(key,pending))return null;
            var output=pending.task().take();if(output==null)return null;
            var result=new StoragePreparationResult(key,pending,output);storageResults.add(result);return result;
        }
        finally {pending.task().close();}
    }
    /** Read an original acknowledged storage intent after re-resolving its live registered storage.
     * This query grants no execution permission and does not settle task, resource or coverage obligations. */
    public synchronized RecoveryFileReadback.Result queryStorageIntent(EffectKey key) {
        storageQueryStatus="RUNTIME_UNAVAILABLE";
        var unavailable=new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
            RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
        var server=runtime.authorityState().server();
        if(closed||server==null||!server.isSameThread()||runtime.authorityState().shuttingDown())return unavailable;
        storageQueryStatus="ORIGINAL_INTENT_UNAVAILABLE";var original=runtime.durableStore().recoveryWorkResult(key);
        if(original==null||original.descriptor().kind()!=RecoveryWorkDescriptor.Kind.STORAGE_COMMIT)return unavailable;
        var description=original.descriptor();var location=description.location();
        storageQueryStatus="ADAPTER_OR_SCOPE_UNAVAILABLE";
        if(description.storageGeneration()==null||description.fileIdentity().isEmpty()
            ||location.scope()!=RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA
            ||!description.adapterHash().equals(StorageAdapterFingerprint.value()))return unavailable;
        try {
            storageQueryStatus="LEVEL_UNAVAILABLE";ServerLevel level=null;
            for(var candidate:server.getAllLevels())if(candidate.dimension().location().toString().equals(location.dimension())){level=candidate;break;}
            if(level==null)return unavailable;
            storageQueryStatus="STORAGE_UNAVAILABLE";var storage=level.getDataStorage();
            if(storage.getClass()!=DimensionDataStorage.class||!(storage instanceof StorageMutationTracked tracked))return unavailable;
            storageQueryStatus="CACHE_UNAVAILABLE";long version=tracked.ronova$cacheObservationVersion();var field=storageCache();var raw=field.get(storage);
            if(version<0||!safeMap(raw))return unavailable;
            storageQueryStatus="HOLDER_UNAVAILABLE";var cache=(Map<?,?>)raw;Object owner=cache.get(location.storageName());
            if(!(owner instanceof SavedData))return unavailable;
            long deadline=System.nanoTime()+50_000_000L;
            storageQueryStatus="LOCATION_UNAVAILABLE";var resolved=RecoveryLocationResolver.loaded(owner,location,key.targetEpoch().subjectKey().subjectId(),()->System.nanoTime()<deadline);
            if(resolved.state()==RecoveryLocationResolver.State.UNAVAILABLE||resolved.state()==RecoveryLocationResolver.State.BUDGET_EXHAUSTED)return unavailable;
            storageQueryStatus="PATH_UNAVAILABLE";var path=tracked.ronova$observedDataFile(location.storageName());if(path==null)return unavailable;
            var charges=Map.of(HostResourceDomain.JAVA_HEAP,65536L,HostResourceDomain.JNI_JNA_NATIVE,65536L,
                HostResourceDomain.FILE_DESCRIPTOR,2L,HostResourceDomain.PLATFORM_HANDLE,2L);
            for(var domain:charges.keySet())runtime.resources().declareAccounting(domain,HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"storage-original-query");
            var allocation=runtime.resources().reserve(charges,HostResourceGovernor.Priority.RECOVERY);
            if(!allocation.granted()){storageQueryStatus="RESOURCE_"+allocation.denial();return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
                RecoveryFileReadback.Reason.RESOURCE_UNAVAILABLE,0,null);}
            storageQueryStatus="PROOF_UNAVAILABLE";
            try(var reservation=allocation.reservation();var proof=runtime.enforcement().beginObservation(0x6000)) {
                if(proof==null||!proof.stillCurrent())return unavailable;
                var result=RecoveryStorageReadbackQuery.query(key,runtime.durableStore()::recoveryWorkResult,
                    ignored->RecoveryFileReadback.read(path,description.expectedHash(),RecoveryFileReadback.MAX_BYTES,()->System.nanoTime()<deadline));
                storageQueryStatus="BINDING_OR_PROOF_CHANGED";
                if(server.getLevel(level.dimension())!=level||level.getDataStorage()!=storage
                    ||tracked.ronova$cacheObservationVersion()!=version||field.get(storage)!=cache||cache.get(location.storageName())!=owner
                    ||!path.equals(tracked.ronova$observedDataFile(location.storageName()))||!proof.finish())return unavailable;
                storageQueryStatus=result.status()+"/"+result.reason();return result;
            }
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unresolved){return unavailable;}
    }
    private String storageQueryStatus="NOT_REQUESTED";
    public synchronized String storageQueryDiagnostic(){return storageQueryStatus;}
    private void reviewStoragePreparations() {
        for(var iterator=closingStorageTransactions.iterator();iterator.hasNext();) {
            var task=iterator.next();task.close();if(task.resourcesReleased())iterator.remove();
        }
        for(var result:List.copyOf(storageResults))if(!result.current())result.close();
        for(var iterator=storagePreparations.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!storagePreparationCurrent(entry.getKey(),entry.getValue())) {
                entry.getValue().task().close();
                if(entry.getValue().task().done())iterator.remove();
            }
        }
    }
    /** Original Root intent supplies the expected image; live source identity must still match its registration. */
    public synchronized RecoveryFileReadback.Result readStorageIntent(EffectKey intentKey,SourceKey sourceKey,
        SavedDataSaveTrace.Snapshot beforeSave,WorkBudget budget) {
        return RecoveryStorageReadbackQuery.query(intentKey,runtime.durableStore()::recoveryWorkResult,description->{
            var source=sources.get(sourceKey);
            if(source==null||!description.location().equals(source.location)
                ||!description.registration().equals(source.mutationIncarnation)
                ||description.sourceVersion()!=source.key.observedVersion()
                ||!intentKey.targetEpoch().equals(source.key.targetEpoch())
                ||!intentKey.canonicalCommandId().equals(source.terminal.commandId()))
                return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
                    RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
            return readObservedStorage(sourceKey,beforeSave,description.expectedHash(),budget);
        });
    }
    /** Read only the file actually observed for this loaded source. This does not write Root completion facts. */
    public synchronized RecoveryFileReadback.Result readObservedStorage(SourceKey key,
        SavedDataSaveTrace.Snapshot beforeSave,String expectedEncodedHash,WorkBudget budget) {
        var source=sources.get(key);
        var unavailable=new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
            RecoveryFileReadback.Reason.PRECONDITION_UNPROVEN,0,null);
        if(source==null||source.saveTrace==null||budget==null||closed)return unavailable;
        var server=source.server.get();
        if(server==null||!server.isSameThread()||!current(source.terminal.qualification()))return unavailable;
        var saved=source.saveTrace.snapshot();
        if(!saved.observedWriteReturnAfter(beforeSave)||saved.observedPath()==null)return unavailable;
        java.util.function.BooleanSupplier bound=()->source.currentStorageWorld()
            &&(source.stillBound()||source.recordBound(true)||source.emptyAtomicSlot());
        var capacity=runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP,65536L,
            HostResourceDomain.THREAD_STACK,65536L,HostResourceDomain.FILE_DESCRIPTOR,2L,
            HostResourceDomain.PLATFORM_HANDLE,2L,HostResourceDomain.DIRECT_BUFFER,32768L,
            HostResourceDomain.JNI_JNA_NATIVE,4096L),HostResourceGovernor.Priority.RECOVERY);
        if(!capacity.granted())return new RecoveryFileReadback.Result(RecoveryFileReadback.Status.UNAVAILABLE,
            RecoveryFileReadback.Reason.RESOURCE_UNAVAILABLE,0,null);
        try(var retained=capacity.reservation();var proof=runtime.enforcement().beginObservation(0x6000)) {
            if(proof==null||!proof.stillCurrent()||!bound.getAsBoolean())return unavailable;
            var result=RecoveryFileReadback.read(java.nio.file.Path.of(saved.observedPath()),expectedEncodedHash,
                RecoveryFileReadback.MAX_BYTES,()->budget.tryConsumeCount(1));
            if(!saved.equals(source.saveTrace.snapshot())||!current(source.terminal.qualification())
                ||!bound.getAsBoolean()||!proof.finish())return unavailable;
            return result;
        }
    }
    public RecoveryTaskObserver taskObserver(){return taskObserver;}
    public synchronized String taskDispositionDiagnostic(){
        var stages=new java.util.LinkedHashMap<String,Long>();
        for(var stage:TaskCandidateStage.values())if(taskCandidateStages[stage.ordinal()]!=0)stages.put(stage.name(),taskCandidateStages[stage.ordinal()]);
        return taskDisposition.diagnostic()+" candidate="+lastTaskCandidateStage+" registration="+lastTaskCandidateRegistration
            +" witness_age_ns="+lastTaskWitnessAgeNanos+" queue_deferrals="+taskDispositionQueueDeferrals
            +" candidate_stages="+stages;
    }
    /** Fault injection can delay one exact task only; discovery and authority still come from production observers. */
    public synchronized boolean delayTaskInvocation(Object task,long nanos) {
        return Thread.currentThread()==observationThread&&!closed&&taskDisposition.delayInvocation(task,nanos);
    }
    public synchronized Map<String,Long> taskDispositionCounters(){return taskDisposition.counters();}
    public synchronized Map<String,Long> reconstructionProofDiagnostic(){return lastReconstructionProof;}
    public synchronized String reconstructionFramesDiagnostic(){return RecoveryBoundaryProof.reconstructionFrames();}
    public synchronized Map<String,Long> bootstrapReconstructionDiagnostic(){return lastBootstrapReconstructionProof;}
    public synchronized long staticSourceDiscoveries() { return staticDiscoveries; }
    public synchronized long clearedStaticSnapshots() { return clearedStaticSnapshots; }
    public synchronized long clearedLoadedSnapshots() { return clearedLoadedSnapshots; }
    public synchronized long unverifiedSnapshotWrites() { return unverifiedSnapshotWrites; }
    public synchronized long removedStaticRecords() { return removedStaticRecords; }
    public synchronized long removedLoadedRecords() { return removedLoadedRecords; }
    public synchronized long unverifiedRecordWrites() { return unverifiedRecordWrites; }
    public synchronized Map<String, Long> recordTaskCounters() {
        long pending = 0, blocked = 0, cancelled = 0, expired = 0, finished = 0;
        for (var work : recordTasks.values()) {
            switch (work.task.state()) {
                case RUNNING -> pending++;
                case CANCEL_PENDING -> { pending++; cancelled++; }
                case EXPIRED_PENDING -> { pending++; expired++; }
                case FINISHED, FAILED -> finished++;
            }
            if (work.task.monitorBlocked()) blocked++;
        }
        return Map.of("record_tasks_pending", pending, "record_tasks_monitor_blocked", blocked,
            "record_tasks_cancel_pending", cancelled, "record_tasks_expired_pending", expired,
            "record_results_uncollected", finished, "record_tasks_submitted", recordTasksSubmitted,
            "record_tasks_rejected", recordTasksRejected, "record_tasks_finished", recordTasksFinished,
            "record_tasks_failed", recordTasksFailed, "mutation_receipts_pending", (long) mutationReceipts.readyCount());
    }
    public synchronized String summary() {
        return "本世界重建拦截 " + heldOutputs + "；快照清空 " + clearedStaticSnapshots
            + "；已加载快照清空 " + clearedLoadedSnapshots + "；静态记录移除 " + removedStaticRecords
            + "；已加载记录移除 " + removedLoadedRecords + "；整链未确认";
    }
    public synchronized Map<String, Long> policyCounters() {
        var rows = dev.ronova.coremod.policy.RecoveryChainPolicy.snapshot(policy());
        var result = new LinkedHashMap<String, Long>();
        rows.forEach((step, decision) -> {
            String name = step.name().toLowerCase(Locale.ROOT);
            result.put(name + "_requested", (long) decision.requested().rank());
            result.put(name + "_effective", (long) decision.effective().rank());
        });
        return Map.copyOf(result);
    }
    @Override public synchronized void close() {
        if (closed) return; closed = true;
        recordTasks.values().forEach(work -> work.task.cancel());
        recordTasks.clear();
        storagePreparations.values().forEach(pending->pending.task().close());storagePreparations.clear();
        for(var result:List.copyOf(storageResults))result.close();
        // Remove retained state before callbacks, and try every release even if an
        // individual accounting callback fails. A second close must not double-release.
        List<Runnable> releases = new ArrayList<>(1 + sources.size() + watches.size());
        releases.add(taskDisposition::close);
        releases.add(taskObserver::close);
        releases.add(copies::close);
        releases.add(mutationReceipts::close);
        releases.add(worldAdmission::close);
        sources.values().forEach(source -> releases.add(source.release));
        watches.values().forEach(watch -> releases.add(watch::close));
        staticScans.forEach(scan -> releases.add(scan::close));
        staticScans.clear();
        sources.clear(); sourceRotation.clear(); watches.clear(); rotation.clear(); policyView = null;
        RuntimeException failure = null;
        for (Runnable release : releases) try { release.run(); }
        catch (RuntimeException error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
    }
    /** Shutdown does not join foreign-monitor waiters or execute a new mutation. */
    public synchronized boolean flushShutdownResults() {
        if (closed) return false; // Cleared memory is not evidence that its receipts were acknowledged.
        if (!runtime.authorityState().shuttingDown()) return false;
        recordTasks.values().forEach(work -> work.task.cancel());
        try { collectRecordTasks(); }
        finally {
            // Completed receipts survive source retirement and are published as one Root image.
            // Still-running tasks keep their original durable unknown intent.
            mutationReceipts.flushBatch(values -> runtime.durableStore().observeRecoveryMutations(values.stream()
                .map(value -> new RecoveryMutationIntent.Receipt(value.getKey(), value.getValue())).toList()));
        }
        return mutationReceipts.size() == 0;
    }
    private static long next(long value) { return value == Long.MAX_VALUE ? value : value + 1; }
}
