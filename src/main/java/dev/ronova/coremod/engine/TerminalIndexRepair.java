package dev.ronova.coremod.engine;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.mixin.*;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.runtime.CompletionTracker;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Resumable object-identity removal from registered world indexes; never calls entity lifecycle methods. */
public final class TerminalIndexRepair {
    static final WriteSinkId UUID_LOOKUP_SINK=new WriteSinkId("ronova:world-records/entity-lookup-uuid");
    static final WriteSinkId KNOWN_UUID_SINK=new WriteSinkId("ronova:world-records/known-uuid");
    static final WriteSinkId NUMERIC_LOOKUP_SINK=new WriteSinkId("ronova:world-records/entity-lookup-id");
    static final WriteSinkId ACTIVE_TICK_SINK=new WriteSinkId("ronova:world-records/tick-active");
    static final WriteSinkId PASSIVE_TICK_SINK=new WriteSinkId("ronova:world-records/tick-passive");
    private static final List<WriteSinkId> INT_INDEX_SINKS=List.of(NUMERIC_LOOKUP_SINK,ACTIVE_TICK_SINK,PASSIVE_TICK_SINK);
    private static final int MAX_LIVE = 128, SLICE_UNITS = 64;
    private static final long SLICE_NANOS = 100_000L;
    private final DominionServerRuntime runtime;
    private record Key(dev.ronova.coremod.contract.SubjectKey subject, UUID nonce) {}
    private final LinkedHashMap<Key, Cursor> cursors = new LinkedHashMap<>();
    private static Key key(TerminalOperationPlan plan) { return new Key(plan.subjectKey(), plan.physicalNonce()); }
    private static final class Cursor {
        final WeakReference<Entity> entity;
        final CompletionTracker.Scope scope;
        final int initialId;
        final HostResourceGovernor.Reservation reservation;
        long nextSection = Long.MIN_VALUE, selectedSection;
        boolean sectionSelected, allInstancesDone, sectionMapGap;
        List<WeakReference<Object>> bindings = List.of();
        WeakReference<Object> section = new WeakReference<>(null), group = new WeakReference<>(null);
        WeakReference<Object> allInstances = new WeakReference<>(null), byClass = new WeakReference<>(null);
        WeakReference<Class<?>> variantKey = new WeakReference<>(null);
        WeakReference<List<?>> list = new WeakReference<>(null);
        WeakReference<Iterator<Map.Entry<Class<?>, List<?>>>> variants = new WeakReference<>(null);
        WriteSetReceipt sliceWriteSet;
        int item = -1;
        int aliasIndex, aliasPosition;
        WeakReference<int[]> aliasKeys=new WeakReference<>(null);
        WeakReference<Object[]> aliasValues=new WeakReference<>(null);
        Cursor(Entity entity, TerminalOperationPlan plan, int initialId, HostResourceGovernor.Reservation reservation) {
            this.entity = new WeakReference<>(entity); scope = CompletionTracker.Scope.of(plan);
            this.initialId = initialId; this.reservation = reservation;
        }
        void resetGroup(Group current) {
            section = new WeakReference<>(current == null ? null : current.section);
            group = new WeakReference<>(current == null ? null : current.group);
            allInstances = new WeakReference<>(current == null ? null : current.all);
            byClass = new WeakReference<>(current == null ? null : current.variants);
            list = new WeakReference<>(null); variantKey = new WeakReference<>(null);
            variants = new WeakReference<>(null); allInstancesDone = false; item = -1;
        }
        void resetAliases() { aliasIndex=aliasPosition=0;aliasKeys.clear();aliasValues.clear(); }
        void resetSweep() { nextSection = Long.MIN_VALUE; sectionSelected = false; resetGroup(null);resetAliases(); }
        void bind(Indexes indexes) {
            Object[] current = indexes.identities();
            boolean same = bindings.size() == current.length;
            for (int i = 0; same && i < current.length; i++) same = bindings.get(i).get() == current[i];
            if (same) return;
            resetSweep();
            var next = new ArrayList<WeakReference<Object>>(current.length);
            for (Object value : current) next.add(new WeakReference<>(value));
            bindings = List.copyOf(next);
        }
        boolean bound(Group current) {
            return section.get() == current.section && group.get() == current.group
                && allInstances.get() == current.all && byClass.get() == current.variants;
        }
    }
    /** Captured references are used for operations; later getters only compare their ownership binding. */
    private static final class Indexes {
        final ServerLevel level;
        final Entity expectedEntity;
        final EntityStateAccessor identity;
        final UUID subject;
        final ServerLevelAccessor world;
        final PersistentEntityManagerAccessor manager;
        final EntityLookupAccessor lookup;
        final EntityTickListAccessor ticks;
        final EntitySectionStorageAccessor storage;
        final Map<UUID, net.minecraft.world.level.entity.EntityAccess> byUuid;
        final it.unimi.dsi.fastutil.ints.Int2ObjectMap<net.minecraft.world.level.entity.EntityAccess> byId;
        final it.unimi.dsi.fastutil.ints.Int2ObjectMap<Entity> active, passive;
        final Set<UUID> known;
        final it.unimi.dsi.fastutil.longs.Long2ObjectMap<net.minecraft.world.level.entity.EntitySection<?>> sections;
        final it.unimi.dsi.fastutil.longs.LongSortedSet sectionIds;
        Indexes(ServerLevel level,Entity entity) {
            this.level = level;
            expectedEntity=entity;identity=access(entity,Entity.class,EntityStateAccessor.class);
            subject=identity.ronova$getUuid();
            if(subject==null||identity.ronova$getLevel()!=level)throw new UnverifiedIndex("entity identity unavailable");
            world = access(level, ServerLevel.class, ServerLevelAccessor.class);
            manager = access(world.ronova$getEntityManager(), net.minecraft.world.level.entity.PersistentEntitySectionManager.class, PersistentEntityManagerAccessor.class);
            lookup = access(manager.ronova$getVisibleStorage(), net.minecraft.world.level.entity.EntityLookup.class, EntityLookupAccessor.class);
            ticks = access(world.ronova$getEntityTickList(), net.minecraft.world.level.entity.EntityTickList.class, EntityTickListAccessor.class);
            storage = access(manager.ronova$getSectionStorage(), net.minecraft.world.level.entity.EntitySectionStorage.class, EntitySectionStorageAccessor.class);
            byUuid = lookup.ronova$getByUuid(); byId = lookup.ronova$getById();
            active = ticks.ronova$getActive(); passive = ticks.ronova$getPassive(); known = manager.ronova$getKnownUuids();
            sections = storage.ronova$getSections(); sectionIds = storage.ronova$getSectionIds();
            collection(byUuid, HashMap.class); collection(known, HashSet.class);
            collection(byId, it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.class);
            collection(active, it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.class);
            collection(passive, it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap.class);
            collection(sections, it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap.class);
            collection(sectionIds, it.unimi.dsi.fastutil.longs.LongAVLTreeSet.class);
        }
        boolean bound(Entity entity) {
            return entity==expectedEntity&&identity.ronova$getLevel()==level&&subject.equals(identity.ronova$getUuid())
                &&level.getServer().isSameThread()&&level.getServer().getLevel(level.dimension())==level
                && (Object) world.ronova$getEntityManager() == manager
                && (Object) world.ronova$getEntityTickList() == ticks && (Object) manager.ronova$getVisibleStorage() == lookup
                && (Object) manager.ronova$getSectionStorage() == storage && manager.ronova$getKnownUuids() == known
                && lookup.ronova$getByUuid() == byUuid && lookup.ronova$getById() == byId
                && ticks.ronova$getActive() == active && ticks.ronova$getPassive() == passive
                && storage.ronova$getSections() == sections && storage.ronova$getSectionIds() == sectionIds;
        }
        boolean writable(Entity entity) { return bound(entity) && ticks.ronova$getIterated() == null; }
        Object[] identities() { return new Object[]{level, manager, lookup, ticks, storage, byUuid, byId, active, passive, known, sections, sectionIds}; }
    }
    private static final class Group {
        final net.minecraft.world.level.entity.EntitySection<?> section;
        final EntitySectionAccessor sectionAccess;
        final ClassInstanceMultiMapAccessor group;
        final List<?> all;
        final Map<Class<?>, List<?>> variants;
        Group(net.minecraft.world.level.entity.EntitySection<?> section) {
            this.section = section;
            sectionAccess = access(section, net.minecraft.world.level.entity.EntitySection.class, EntitySectionAccessor.class);
            group = access(sectionAccess.ronova$getStorage(), net.minecraft.util.ClassInstanceMultiMap.class, ClassInstanceMultiMapAccessor.class);
            all = group.ronova$getAllInstances(); variants = group.ronova$getByClass();
            collection(all, ArrayList.class); collection(variants, HashMap.class);
        }
        boolean bound(Indexes indexes, long position, Entity entity) {
            return indexes.writable(entity) && indexes.sections.get(position) == section
                && (Object) sectionAccess.ronova$getStorage() == group && group.ronova$getAllInstances() == all
                && group.ronova$getByClass() == variants;
        }
        boolean containsList(Cursor cursor, List<?> candidate) {
            if (!cursor.allInstancesDone) return candidate == all;
            Class<?> key = cursor.variantKey.get();
            return key != null && variants.get(key) == candidate;
        }
    }
    private static final class UnverifiedIndex extends IllegalStateException {
        UnverifiedIndex(String message) { super(message); }
    }

    public TerminalIndexRepair(DominionServerRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
        runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
            "terminal-index-cursor/2048-bytes");
    }
    /** Reserve the cursor before opaque callbacks can change the object's numeric ID. */
    public synchronized boolean prepare(Entity entity, TerminalOperationPlan plan, int observedInitialId) {
        if(entity==null||plan==null||runtime.durableStore()==null||!runtime.durableStore().validateTerminalPlan(plan)
            ||!runtime.incarnationNonce(entity).equals(plan.physicalNonce()))return false;
        cursors.entrySet().removeIf(entry -> {
            Cursor cursor = entry.getValue(); Entity physical = cursor.entity.get();
            var qualification = runtime.durableStore().subjectQualification(entry.getKey().subject());
            if (physical != null
                && qualification != null && qualification.terminal()
                && qualification.targetEpoch().equals(cursor.scope.qualification().targetEpoch())
                && qualification.revisions()!=null&&qualification.revisions().activeIncarnation()!=null
                && qualification.revisions().activeIncarnation().nonce().equals(entry.getKey().nonce())) return false;
            cursor.reservation.close(); return true;
        });
        Cursor found = cursors.get(key(plan));
        int initialId = observedInitialId;
        if (found != null) {
            if (found.entity.get() == entity && found.scope.equals(CompletionTracker.Scope.of(plan))) return true;
            if(found.entity.get()==entity)initialId=found.initialId;
            found.reservation.close(); cursors.remove(key(plan));
        }
        if (cursors.size() >= MAX_LIVE) return false;
        var request = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, 2048L), HostResourceGovernor.Priority.RECOVERY);
        if (!request.granted()) return false;
        cursors.put(key(plan), new Cursor(entity, plan, initialId, request.reservation())); return true;
    }
    public synchronized OperationReport.StepReceipt repair(Entity entity, TerminalOperationPlan plan, BooleanSupplier permitted) {
        Cursor sliceCursor=plan==null?null:cursors.get(key(plan));
        if(sliceCursor!=null)sliceCursor.sliceWriteSet=null;
        try {
            var result = repairRegistered(entity, plan, permitted);
            if (result.status() == OperationReport.Status.FENCED) {
                Cursor cursor = cursors.get(key(plan)); if (cursor != null) cursor.resetSweep();
            }
            var writeSet=sliceCursor==null?null:sliceCursor.sliceWriteSet;
            return writeSet==null?result:new OperationReport.StepReceipt(result.step(),result.status(),result.evidence(),writeSet);
        }
        catch (RuntimeException | LinkageError unavailable) {
            Cursor cursor = plan==null?null:cursors.get(key(plan)); if (cursor != null) cursor.resetSweep();
            var result=receipt(OperationReport.Status.UNAVAILABLE, "index-accessor-or-container-semantics-unverified");
            var writeSet=sliceCursor==null?null:sliceCursor.sliceWriteSet;
            return writeSet==null?result:new OperationReport.StepReceipt(result.step(),result.status(),result.evidence(),writeSet);
        }
    }
    private OperationReport.StepReceipt repairRegistered(Entity entity, TerminalOperationPlan plan, BooleanSupplier permitted) {
        if(entity==null||plan==null||runtime.durableStore()==null||!runtime.durableStore().validateTerminalPlan(plan)
            ||!runtime.incarnationNonce(entity).equals(plan.physicalNonce()))
            return receipt(OperationReport.Status.FENCED,"index-plan-or-incarnation-invalid");
        Cursor cursor = cursors.get(key(plan));
        if (cursor == null || cursor.entity.get() != entity || !cursor.scope.equals(CompletionTracker.Scope.of(plan)))
            return receipt(OperationReport.Status.UNAVAILABLE, "index-cursor-not-reserved");
        var identity=access(entity,Entity.class,EntityStateAccessor.class);
        if (!permitted.getAsBoolean() || !(identity.ronova$getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()
            ||!plan.subjectKey().subjectId().equals(identity.ronova$getUuid())
            ||dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(level.getServer())!=runtime)
            return receipt(OperationReport.Status.FENCED, "index-write-permission-invalid");
        Indexes indexes = new Indexes(level,entity);
        if (!indexes.bound(entity)) return changed(cursor);
        cursor.bind(indexes);
        if (indexes.ticks.ronova$getIterated() != null)
            return receipt(OperationReport.Status.DISPATCHED, "index-repair-waits-for-active-tick-iteration");
        // Exact registered containers and exact entity identity, checked after the permission adapter.
        if (!permitted.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "index-slot-write-revoked");
        if (!indexes.writable(entity)) return changed(cursor);
        var id = plan.subjectKey().subjectId();
        if (indexes.byUuid.get(id) == entity) {
            try(var writeSet=WriteSetLease.forPlan(plan,permitted,UUID_LOOKUP_SINK)) {
                if(!writeSet.consume(UUID_LOOKUP_SINK)) { retainWriteReceipt(cursor,writeSet.receipt());
                    return receipt(OperationReport.Status.FENCED,"write-set-uuid-lookup-rejected"); }
                if(!indexes.writable(entity)||indexes.byUuid.get(id)!=entity)return changed(cursor);
                indexes.byUuid.remove(id);
                retainWriteReceipt(cursor,writeSet.receipt());
            }
            // Keep the paired UUID bookkeeping adjacent, but give the second physical container
            // its own final-write sink so a partial pair is never reported as one exact outlet.
            if (!indexes.writable(entity)) return changed(cursor);
            if (indexes.byUuid.get(id) == null && indexes.known.contains(id)) {
                try(var writeSet=WriteSetLease.forPlan(plan,permitted,KNOWN_UUID_SINK)) {
                    if(!writeSet.consume(KNOWN_UUID_SINK)) { retainWriteReceipt(cursor,writeSet.receipt());
                        return receipt(OperationReport.Status.FENCED,"write-set-known-uuid-rejected"); }
                    if(!indexes.writable(entity)||indexes.byUuid.get(id)!=null||!indexes.known.contains(id))return changed(cursor);
                    indexes.known.remove(id);
                    retainWriteReceipt(cursor,writeSet.receipt());
                }
            }
        }
        int currentId = identity.ronova$getEntityId();
        var intMaps=List.<it.unimi.dsi.fastutil.ints.Int2ObjectMap<?>>of(indexes.byId,indexes.active,indexes.passive);
        for (int key : new int[]{cursor.initialId, currentId}) {
            for (int mapIndex=0;mapIndex<intMaps.size();mapIndex++) {
                var map=intMaps.get(mapIndex);
                if (!permitted.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "index-slot-write-revoked");
                if (!indexes.writable(entity)) return changed(cursor);
                if (map.get(key) != entity) continue;
                var sink=INT_INDEX_SINKS.get(mapIndex);
                try(var writeSet=WriteSetLease.forPlan(plan,permitted,sink)) {
                    if(!writeSet.consume(sink)) { retainWriteReceipt(cursor,writeSet.receipt());
                        return receipt(OperationReport.Status.FENCED,"write-set-int-index-rejected"); }
                    if(!indexes.writable(entity)||map.get(key)!=entity)return changed(cursor);
                    map.remove(key);
                    retainWriteReceipt(cursor,writeSet.receipt());
                }
            }
        }
        if (!indexes.writable(entity)) return changed(cursor);
        cursor.sectionMapGap |= indexes.sections.size() != indexes.sectionIds.size();
        long started = System.nanoTime(); int units = 0;
        try {
            while (units++ < SLICE_UNITS && System.nanoTime() - started < SLICE_NANOS) {
                if(cursor.aliasIndex<3) {
                    if(!indexes.writable(entity))return changed(cursor);
                    var map=intMaps.get(cursor.aliasIndex);
                    var sink=INT_INDEX_SINKS.get(cursor.aliasIndex);
                    var slots=RegisteredIntIndexSlots.read(map);
                    if(slots==null)throw new UnverifiedIndex("numeric slot layout unavailable");
                    if(cursor.aliasKeys.get()!=slots.keys()||cursor.aliasValues.get()!=slots.values()) {
                        cursor.aliasPosition=0;cursor.aliasKeys=new WeakReference<>(slots.keys());cursor.aliasValues=new WeakReference<>(slots.values());
                    }
                    if(cursor.aliasPosition>=slots.values().length) {
                        cursor.aliasIndex++;cursor.aliasPosition=0;cursor.aliasKeys.clear();cursor.aliasValues.clear();continue;
                    }
                    int position=cursor.aliasPosition++;
                    if(slots.values()[position]==entity) {
                        if(!permitted.getAsBoolean())return receipt(OperationReport.Status.FENCED,"alias-write-revoked");
                        try(var writeSet=WriteSetLease.forPlan(plan,permitted,sink)) {
                            if(!writeSet.consume(sink)) { retainWriteReceipt(cursor,writeSet.receipt());
                                return receipt(OperationReport.Status.FENCED,"write-set-int-alias-rejected"); }
                            if(!indexes.writable(entity)||!slots.bound()||slots.values()[position]!=entity)return changed(cursor);
                            if(slots.removeIfSame(position,entity)) {
                                cursor.aliasPosition=position;
                                retainWriteReceipt(cursor,writeSet.receipt());
                            } else return changed(cursor);
                        }
                    }
                    continue;
                }
                if (!permitted.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "section-write-revoked");
                if (!indexes.writable(entity)) return changed(cursor);
                if (!cursor.sectionSelected) {
                    var positions = indexes.sectionIds.tailSet(cursor.nextSection).iterator();
                    if (!positions.hasNext()) return finish(entity, plan, cursor, indexes, permitted);
                    cursor.selectedSection = positions.nextLong(); cursor.sectionSelected = true; cursor.resetGroup(null);
                }
                var section = indexes.sections.get(cursor.selectedSection);
                if (section == null) {
                    cursor.sectionMapGap = true;
                    if (!advance(cursor)) return finish(entity, plan, cursor, indexes, permitted);
                    continue;
                }
                Group group = new Group(section);
                if (!group.bound(indexes, cursor.selectedSection, entity)) return changed(cursor);
                if (!cursor.bound(group)) cursor.resetGroup(group);
                List<?> list = cursor.list.get();
                if (list != null && !group.containsList(cursor, list)) { cursor.resetGroup(group); continue; }
                if (list == null) {
                    if (!cursor.allInstancesDone) list = group.all;
                    else {
                        var variants = cursor.variants.get();
                        if (variants == null) { variants = group.variants.entrySet().iterator(); cursor.variants = new WeakReference<>(variants); }
                        if (!variants.hasNext()) {
                            if (!group.bound(indexes, cursor.selectedSection, entity)) return changed(cursor);
                            if (!advance(cursor)) return finish(entity, plan, cursor, indexes, permitted);
                            continue;
                        }
                        var variant = variants.next();
                        cursor.variantKey = new WeakReference<>(variant.getKey()); list = variant.getValue();
                    }
                    collection(list, ArrayList.class);
                    if (!group.containsList(cursor, list)) { cursor.resetGroup(group); continue; }
                    cursor.list = new WeakReference<>(list); cursor.item = list.size() - 1;
                }
                cursor.item = Math.min(cursor.item, list.size() - 1);
                if (cursor.item < 0) {
                    cursor.allInstancesDone = true; cursor.list = new WeakReference<>(null); cursor.variantKey = new WeakReference<>(null); continue;
                }
                int position = cursor.item--;
                if (list.get(position) == entity) {
                    if (!permitted.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "section-entry-write-revoked");
                    if (!group.bound(indexes, cursor.selectedSection, entity)) return changed(cursor);
                    if (!group.containsList(cursor, list)) { cursor.resetGroup(group); continue; }
                    // Recheck the selected slot after permission and ownership checks, using the captured list.
                    if (position < list.size() && list.get(position) == entity) list.remove(position);
                }
                if (!group.bound(indexes, cursor.selectedSection, entity)) return changed(cursor);
                if (!group.containsList(cursor, list)) cursor.resetGroup(group);
            }
        } catch (ConcurrentModificationException | IndexOutOfBoundsException changed) {
            cursor.resetSweep();
            return receipt(OperationReport.Status.DISPATCHED, "section-changed-rescan-current-storage");
        }
        if (!indexes.writable(entity)) return changed(cursor);
        return receipt(OperationReport.Status.DISPATCHED, "section-sweep-pending-next-budget-slice");
    }
    private static void retainWriteReceipt(Cursor cursor,WriteSetReceipt receipt) {
        if(cursor==null||receipt==null)return;
        cursor.sliceWriteSet=WriteSetReceipt.combine(java.util.stream.Stream.of(cursor.sliceWriteSet,receipt)
            .filter(java.util.Objects::nonNull).toList());
    }
    private static OperationReport.StepReceipt changed(Cursor cursor) {
        cursor.resetSweep();
        return receipt(OperationReport.Status.DISPATCHED, "index-container-binding-changed-rescan-current-storage");
    }
    private static boolean advance(Cursor cursor) {
        cursor.sectionSelected = false;
        if (cursor.selectedSection == Long.MAX_VALUE) return false;
        cursor.nextSection = cursor.selectedSection + 1; return true;
    }
    private OperationReport.StepReceipt finish(Entity entity, TerminalOperationPlan plan, Cursor cursor, Indexes indexes, BooleanSupplier permitted) {
        if (!permitted.getAsBoolean()) return receipt(OperationReport.Status.FENCED, "index-pass-final-permission-revoked");
        if (!indexes.writable(entity)) return changed(cursor);
        cursors.remove(key(plan)); cursor.reservation.close();
        // A completed sweep is not a proof that arbitrary concurrent/direct reinsert sources are controlled.
        return receipt(OperationReport.Status.DISPATCHED, cursor.sectionMapGap
            ? "registered-index-pass-finished-section-coverage-gap"
            : "registered-index-pass-finished-future-reinsert-proof-pending");
    }
    public synchronized boolean pending(Entity entity) {
        return entity!=null&&cursors.values().stream().anyMatch(cursor->cursor.entity.get()==entity);
    }
    public synchronized boolean pending(TerminalOperationPlan plan) {
        if(plan==null)return false;
        var cursor=cursors.get(key(plan));
        return cursor!=null&&cursor.scope.qualification().targetEpoch().equals(plan.qualification().targetEpoch());
    }
    public static boolean published(Entity entity) {
        try { return publishedRegistered(entity); }
        catch (RuntimeException | LinkageError unavailable) { return true; } // Unknown presence keeps the watch pending.
    }
    private static boolean publishedRegistered(Entity entity) {
        var identity=access(entity,Entity.class,EntityStateAccessor.class);
        if (!(identity.ronova$getLevel() instanceof ServerLevel level)) return true;
        Indexes indexes = new Indexes(level,entity);
        if (!indexes.bound(entity)) return true;
        int id = identity.ronova$getEntityId();
        UUID uuid = indexes.subject;
        boolean published = indexes.byUuid.get(uuid) == entity || indexes.byId.get(id) == entity
            || indexes.active.get(id) == entity || indexes.passive.get(id) == entity;
        return published || !indexes.bound(entity);
    }

    private static <T> T access(Object receiver, Class<?> owner, Class<T> api) {
        if (!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(receiver, owner, api))
            throw new UnverifiedIndex("raw accessor virtual dispatch is not registered");
        return api.cast(receiver);
    }
    private static void collection(Object value, Class<?> implementation) {
        if (value == null || value.getClass() != implementation)
            throw new UnverifiedIndex("unregistered collection implementation");
    }
    public synchronized void clear() {
        var remaining=List.copyOf(cursors.values());cursors.clear();Throwable failed=null;
        for(var cursor:remaining)try {cursor.reservation.close();}
        catch(RuntimeException|Error failure) {
            if(failed==null)failed=failure;else if(failed!=failure)failed.addSuppressed(failure);
        }
        if(failed instanceof RuntimeException error)throw error;
        if(failed instanceof Error error)throw error;
    }
    private static OperationReport.StepReceipt receipt(OperationReport.Status status, String text) {
        return new OperationReport.StepReceipt(TerminalOperationPlan.Step.WORLD_RECORDS, status, text);
    }
}
