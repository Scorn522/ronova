package dev.ronova.coremod.obligation;

import dev.ronova.coremod.contract.SubjectKey;
import java.util.*;

/** Bounded execution diagnostics and fair review queue; it never grants Judgment or clears durable effects. */
public final class ObligationRegistry {
    public enum Candidate { OBSERVED, CANDIDATE }
    public enum State { UNRESOLVED, NEEDS_REVALIDATION }
    public record View(ObligationKey key, long policyRevision, long judgmentRevision, Candidate candidate, State state,
        long count, boolean countSaturated, long firstSeen, long lastSeen, long reviewCount, List<CandidateEvidence> samples,
        Set<String> durableEvidenceRefs, String latestEvidenceDigest, boolean mixedContext) {}
    public record Coverage(long overflowCount, boolean needsRevalidation, String authorityAnchor) {}
    private static final class Entry {
        final ObligationKey key; final long firstSeen;
        long policy, judgment, count, lastSeen, reviews;
        Candidate candidate = Candidate.OBSERVED; State state = State.UNRESOLVED;
        String digest = "";
        boolean mixedContext;
        long revalidationBarrier=-1;
        dev.ronova.coremod.budget.HostResourceGovernor.Reservation reservation;
        final ArrayDeque<CandidateEvidence> samples = new ArrayDeque<>();
        final LinkedHashSet<String> durable = new LinkedHashSet<>();
        Entry(CandidateEvidence evidence) { key=evidence.key(); policy=evidence.policyRevision(); judgment=evidence.judgmentRevision(); firstSeen=evidence.observedAt(); }
        Entry(View view) {key=view.key();firstSeen=view.firstSeen();}
    }
    /** Each subject gets one turn, then each registered source gets one turn within that subject. */
    private static final class SubjectQueue {
        final LinkedHashMap<ObligationKey.Source, ArrayDeque<ObligationKey>> sources = new LinkedHashMap<>();
        void add(ObligationKey key) {
            sources.computeIfAbsent(key.source(), ignored -> new ArrayDeque<>()).addLast(key);
        }
        boolean remove(ObligationKey key) {
            var queue=sources.get(key.source());
            if(queue!=null) { queue.remove(key); if(queue.isEmpty())sources.remove(key.source()); }
            return sources.isEmpty();
        }
        ObligationKey next() {
            var first = sources.entrySet().iterator().next();
            var source = first.getKey(); var queue = first.getValue();
            sources.remove(source); sources.put(source, queue);
            var key = queue.removeFirst(); queue.addLast(key); return key;
        }
    }
    private final int limit;
    private final LinkedHashMap<ObligationKey, Entry> entries = new LinkedHashMap<>();
    private final LinkedHashMap<SubjectKey, SubjectQueue> subjects = new LinkedHashMap<>();
    private final ArrayDeque<ObligationKey> reclaimQueue = new ArrayDeque<>();
    private final LinkedHashMap<ObligationKey, Entry> retryEntries = new LinkedHashMap<>();
    private final LinkedHashMap<dev.ronova.coremod.root.api.TargetEpoch, OverflowCheckpoint.Scope> overflowScopes = new LinkedHashMap<>();
    private final Map<dev.ronova.coremod.root.api.TargetEpoch, dev.ronova.coremod.budget.HostResourceGovernor.Reservation> scopeReservations = new HashMap<>();
    private boolean restorePending;
    private long unscopedOverflow;
    private long overflow;
    private long version;
    private dev.ronova.coremod.budget.HostResourceGovernor resources;
    public ObligationRegistry() { this(512); }
    public ObligationRegistry(int limit) { if (limit < 1 || limit > 65536) throw new IllegalArgumentException("invalid obligation limit"); this.limit=limit; }
    public ObligationRegistry(dev.ronova.coremod.budget.HostResourceGovernor resources) {
        this(); this.resources=Objects.requireNonNull(resources);
        resources.declareAccounting(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,
            dev.ronova.coremod.budget.HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,"obligation-detail/4096-bytes");
    }
    public synchronized void observe(CandidateEvidence evidence) { observe(evidence, null); }
    public synchronized void observe(CandidateEvidence evidence, String durableRef) {
        Objects.requireNonNull(evidence);
        if(restorePending)throw new IllegalStateException("obligation restore is still pending");
        if (durableRef != null && (durableRef.isBlank() || durableRef.length() > 256)) throw new IllegalArgumentException("unbounded durable reference");
        Entry entry=entry(evidence.key());
        if (entry==null) {
            boolean primary=entries.size()<limit || reclaimExpiredObservation(evidence.observedAt(),false);
            var reservation=primary?reserve(4096,false):null;
            if(primary&&resources!=null&&reservation==null)primary=false;
            if(!primary) {
                overflow=increment(overflow);version=increment(version);
                boolean retryRoom=retryEntries.size()<OverflowCheckpoint.MAX_RETRY_ENTRIES
                    ||reclaimExpiredObservation(evidence.observedAt(),true);
                reservation=retryRoom?reserve(4096,true):null;
                if(!retryRoom||resources!=null&&reservation==null) { recordOverflowScope(evidence);return; }
            }
            entry=new Entry(evidence);entry.reservation=reservation;
            (primary?entries:retryEntries).put(entry.key,entry);reclaimQueue.addLast(entry.key);
            subjects.computeIfAbsent(entry.key.targetEpoch().subjectKey(), ignored->new SubjectQueue()).add(entry.key);
        } else if(retryEntries.containsKey(entry.key)) {
            if(entries.size()<limit||reclaimExpiredObservation(evidence.observedAt(),false)) {
                retryEntries.remove(entry.key);entries.put(entry.key,entry);version=increment(version);
            } else { overflow=increment(overflow);version=increment(version); }
        }
        boolean same=entry.policy==evidence.policyRevision()&&entry.judgment==evidence.judgmentRevision();
        if (!same) {
            // Candidate records historical admission; only fresh context proof may return its state to UNRESOLVED.
            entry.state=State.NEEDS_REVALIDATION;entry.mixedContext=true;
            // A -> B -> A must not re-admit A's delayed strong observations from before the switch.
            entry.revalidationBarrier=Math.max(entry.revalidationBarrier,entry.lastSeen);
            entry.samples.clear();entry.policy=evidence.policyRevision();entry.judgment=evidence.judgmentRevision();
        }
        if(durableRef!=null&&entry.durable.contains(durableRef)) {
            // Context invalidation is itself checkpoint content, even when this durable fact was seen before.
            if (!same) version=increment(version);
            return;
        }
        version=increment(version);
        entry.count=increment(entry.count);entry.lastSeen=Math.max(entry.lastSeen,evidence.observedAt());
        // Restored or explicitly invalidated observations may remain in durable diagnostics, but cannot
        // be reintroduced into a live proof by replaying them alongside one fresh observation.
        long barrier=entry.revalidationBarrier;
        if (evidence.observedAt()>barrier
            && entry.samples.stream().noneMatch(old->old.eventRef().equals(evidence.eventRef())&&old.observedAt()>barrier)) {
            // A fresh attestation may reuse an event identifier after restart, never an old attestation itself.
            entry.samples.removeIf(old->old.eventRef().equals(evidence.eventRef())&&old.observedAt()<=barrier);
            if (entry.samples.size()>=4)entry.samples.removeFirst();entry.samples.addLast(evidence);
            entry.digest=dev.ronova.coremod.root.api.EffectRecord.hash(evidence.toString());
        }
        if (durableRef!=null && entry.durable.size()<4)entry.durable.add(durableRef);
        if(admissible(entry,entry.lastSeen)){entry.candidate=Candidate.CANDIDATE;entry.state=State.UNRESOLVED;}
        else if(entry.candidate==Candidate.CANDIDATE)entry.state=State.NEEDS_REVALIDATION;
    }
    /** Reclaim diagnostic-only detail, never admitted obligations or durable effects; the coverage gap stays recorded. */
    private boolean reclaimExpiredObservation(long now,boolean retryTier) {
        int visits=Math.min(8,reclaimQueue.size());
        for(int i=0;i<visits;i++) {
            var key=reclaimQueue.removeFirst();var entry=entry(key);
            // Missing samples do not prove expiry. Mixed old checkpoints may predate sticky Candidate history.
            boolean expired=retryEntries.containsKey(key)==retryTier && entry.candidate==Candidate.OBSERVED && !entry.mixedContext && entry.durable.isEmpty()
                && !entry.samples.isEmpty() && entry.lastSeen<now && entry.samples.stream().allMatch(sample->sample.validUntil()<now);
            if(!expired) { reclaimQueue.addLast(key);continue; }
            (retryTier?retryEntries:entries).remove(key);
            var subject=key.targetEpoch().subjectKey();
            if(subjects.get(subject).remove(key))subjects.remove(subject);
            if(entry.reservation!=null)entry.reservation.close();
            version=increment(version);
            return true;
        }
        return false;
    }
    private Entry entry(ObligationKey key) {
        var primary=entries.get(key);return primary!=null?primary:retryEntries.get(key);
    }
    private dev.ronova.coremod.budget.HostResourceGovernor.Reservation reserve(long bytes,boolean recovery) {
        if(resources==null)return null;
        var request=resources.reserve(Map.of(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,bytes),
            recovery?dev.ronova.coremod.budget.HostResourceGovernor.Priority.RECOVERY
                :dev.ronova.coremod.budget.HostResourceGovernor.Priority.NORMAL);
        return request.granted()?request.reservation():null;
    }
    private void recordOverflowScope(CandidateEvidence evidence) {
        recordScope(evidence.key().targetEpoch(),evidence.key().source(),evidence.observedAt());
    }
    /** An observer coverage loss is pending scope, never a fabricated Candidate witness. */
    public synchronized void recordCoverageGap(dev.ronova.coremod.root.api.TargetEpoch target,ObligationKey.Source source,long now) {
        Objects.requireNonNull(target);Objects.requireNonNull(source);
        if(restorePending||now<0)throw new IllegalStateException("coverage gap cannot be recorded in this context");
        overflow=increment(overflow);version=increment(version);recordScope(target,source,now);
    }
    private void recordScope(dev.ronova.coremod.root.api.TargetEpoch target,ObligationKey.Source source,long now) {
        var previous=overflowScopes.get(target);
        if(previous==null) {
            if(overflowScopes.size()>=OverflowCheckpoint.MAX_SCOPES) { unscopedOverflow=increment(unscopedOverflow);return; }
            var reservation=reserve(512,true);
            if(resources!=null&&reservation==null) { unscopedOverflow=increment(unscopedOverflow);return; }
            if(reservation!=null)scopeReservations.put(target,reservation);
            overflowScopes.put(target,new OverflowCheckpoint.Scope(target,Set.of(source),1,now,now));
        } else {
            var sources=EnumSet.copyOf(previous.sources());sources.add(source);
            overflowScopes.put(target,new OverflowCheckpoint.Scope(target,sources,increment(previous.count()),
                previous.firstSeen(),Math.max(previous.lastSeen(),now)));
        }
    }
    private static boolean admissible(Entry entry,long now) {
        List<CandidateEvidence> valid=entry.samples.stream().filter(e->e.observerVerified()&&!e.authorizedCompliant()
            && e.observedAt()>entry.revalidationBarrier && e.observedAt()<=now && e.validUntil()>=now
            && e.policyRevision()==entry.policy && e.judgmentRevision()==entry.judgment).toList();
        if(valid.stream().anyMatch(e->e.family().strong()))return true;
        for(int i=0;i<valid.size();i++)for(int j=i+1;j<valid.size();j++) {
            var a=valid.get(i);var b=valid.get(j);
            if(a.family()!=b.family()&&!a.eventRef().equals(b.eventRef())&&!a.basisRef().equals(b.basisRef()))return true;
        }
        return false;
    }
    /** Fair across subjects and source families; a source flood cannot consume every turn of its subject. */
    public synchronized View nextReview(long now) {
        if(subjects.isEmpty())return null;
        var first=subjects.entrySet().iterator().next();SubjectKey subject=first.getKey();SubjectQueue queue=first.getValue();
        subjects.remove(subject);subjects.put(subject,queue);
        ObligationKey key=queue.next();Entry entry=entry(key);
        if(retryEntries.containsKey(key)&&(entries.size()<limit||reclaimExpiredObservation(now,false))) {
            retryEntries.remove(key);entries.put(key,entry); // Same evidence and reservation; no synthetic re-observation.
        }
        entry.reviews=increment(entry.reviews);
        version=increment(version);
        if(!admissible(entry,now))entry.state=State.NEEDS_REVALIDATION;
        return view(entry);
    }
    public synchronized void invalidate(ObligationKey key) {
        Entry entry=entry(key);
        if(entry==null)return;
        if(entry.state!=State.NEEDS_REVALIDATION || entry.revalidationBarrier<entry.lastSeen
            ||entry.samples.stream().anyMatch(CandidateEvidence::observerVerified)) {
            entry.state=State.NEEDS_REVALIDATION;
            entry.revalidationBarrier=Math.max(entry.revalidationBarrier,entry.lastSeen);
            var historical=entry.samples.stream().map(ObligationRegistry::historical).toList();
            entry.samples.clear();entry.samples.addAll(historical);
            version=increment(version);
        }
    }
    /** Retires runtime detail only after an acknowledged newer TargetEpoch; it never resolves a Root effect. */
    public synchronized boolean retireSuperseded(ObligationKey key,dev.ronova.coremod.root.api.SubjectQualification current) {
        if(restorePending||!superseded(key.targetEpoch(),current))return false;
        Entry removed=entries.remove(key);if(removed==null)removed=retryEntries.remove(key);
        if(removed==null)return false;
        reclaimQueue.remove(key);var subject=key.targetEpoch().subjectKey();
        var queue=subjects.get(subject);if(queue!=null&&queue.remove(key))subjects.remove(subject);
        version=increment(version);
        if(removed.reservation!=null)removed.reservation.close();
        return true;
    }
    public synchronized OverflowCheckpoint.Scope nextOverflowScope() {
        if(restorePending||overflowScopes.isEmpty())return null;
        var first=overflowScopes.entrySet().iterator().next();var key=first.getKey();var scope=first.getValue();
        overflowScopes.remove(key);overflowScopes.put(key,scope);return scope;
    }
    public synchronized boolean retireSupersededScope(dev.ronova.coremod.root.api.TargetEpoch target,
        dev.ronova.coremod.root.api.SubjectQualification current) {
        if(restorePending||!superseded(target,current)||overflowScopes.remove(target)==null)return false;
        var permit=scopeReservations.remove(target);version=increment(version);if(permit!=null)permit.close();return true;
    }
    private static boolean superseded(dev.ronova.coremod.root.api.TargetEpoch target,
        dev.ronova.coremod.root.api.SubjectQualification current) {
        return current!=null&&target.subjectKey().equals(current.subjectKey())&&target.generation()<current.targetEpoch().generation();
    }
    public synchronized long version() {return version;}
    public synchronized boolean restore(ObligationCheckpoint checkpoint) {return restore(checkpoint,0);}
    /** Missed observations during allocation failure become an explicit gap before recovery becomes visible. */
    public synchronized boolean restore(ObligationCheckpoint checkpoint,long missedObservations) {
        Objects.requireNonNull(checkpoint);
        if(missedObservations<0)throw new IllegalArgumentException("negative missed observations");
        restorePending=true;
        if(checkpoint.entries().size()>limit)return false;
        var staged=new ArrayList<Entry>();
        var addedReservations=new ArrayList<dev.ronova.coremod.budget.HostResourceGovernor.Reservation>();
        var reusedReservations=new HashSet<dev.ronova.coremod.budget.HostResourceGovernor.Reservation>();
        var stagedScopes=new HashMap<dev.ronova.coremod.root.api.TargetEpoch,dev.ronova.coremod.budget.HostResourceGovernor.Reservation>();
        for(var view:checkpoint.allEntries()) {
            Entry entry=new Entry(view);
            if(resources!=null) {
                var previous=entry(view.key());
                if(previous!=null&&previous.reservation!=null) {
                    entry.reservation=previous.reservation;reusedReservations.add(entry.reservation);
                } else {
                    entry.reservation=reserve(4096,true);
                    if(entry.reservation==null){addedReservations.forEach(dev.ronova.coremod.budget.HostResourceGovernor.Reservation::close);return false;}
                    addedReservations.add(entry.reservation);
                }
            }
            entry.policy=view.policyRevision();entry.judgment=view.judgmentRevision();entry.count=view.count();entry.lastSeen=view.lastSeen();entry.reviews=view.reviewCount();
            entry.candidate=view.candidate();entry.state=State.NEEDS_REVALIDATION;entry.digest=view.latestEvidenceDigest();entry.mixedContext=view.mixedContext();
            // Keep expiry evidence so never-admitted diagnostics can retire after restart, but revoke
            // every old observer attestation. A new witness cannot authorize these historical samples.
            view.samples().stream().map(ObligationRegistry::historical).forEach(entry.samples::addLast);
            entry.revalidationBarrier=checkpoint.clock();
            entry.durable.addAll(view.durableEvidenceRefs());staged.add(entry);
        }
        for(var scope:checkpoint.overflow().scopes()) {
            var reservation=scopeReservations.get(scope.target());
            if(reservation!=null)reusedReservations.add(reservation);
            else {
                reservation=reserve(512,true);
                if(resources!=null&&reservation==null) {
                    addedReservations.forEach(dev.ronova.coremod.budget.HostResourceGovernor.Reservation::close);return false;
                }
                if(reservation!=null)addedReservations.add(reservation);
            }
            if(reservation!=null)stagedScopes.put(scope.target(),reservation);
        }
        // Transfer borrowed charges only after staging succeeds; failed restores leave the old registry intact.
        java.util.stream.Stream.concat(entries.values().stream(),retryEntries.values().stream())
            .forEach(old->{if(reusedReservations.contains(old.reservation))old.reservation=null;});
        scopeReservations.entrySet().removeIf(item->reusedReservations.contains(item.getValue()));
        clear();overflow=checkpoint.coverage().overflowCount();unscopedOverflow=checkpoint.overflow().unscopedCount();
        var primaryKeys=new HashSet<ObligationKey>();checkpoint.entries().forEach(view->primaryKeys.add(view.key()));
        for(var entry:staged){(primaryKeys.contains(entry.key)?entries:retryEntries).put(entry.key,entry);reclaimQueue.addLast(entry.key);subjects.computeIfAbsent(entry.key.targetEpoch().subjectKey(),ignored->new SubjectQueue()).add(entry.key);}
        checkpoint.overflow().scopes().forEach(scope->overflowScopes.put(scope.target(),scope));scopeReservations.putAll(stagedScopes);
        overflow=add(overflow,missedObservations);unscopedOverflow=add(unscopedOverflow,missedObservations);
        version=increment(version);return true;
    }
    public synchronized List<View> snapshot() { return entries.values().stream().map(ObligationRegistry::view).toList(); }
    public synchronized OverflowCheckpoint overflowCheckpoint() {
        return new OverflowCheckpoint(retryEntries.values().stream().map(ObligationRegistry::view).toList(),List.copyOf(overflowScopes.values()),unscopedOverflow);
    }
    public synchronized int retrySize() { return retryEntries.size(); }
    public synchronized Coverage coverage() { return new Coverage(overflow,overflow>0,"Root qualifications/effect journal retained; overflow has no expanded enforcement scope"); }
    public synchronized int size() { return entries.size(); }
    public synchronized boolean restorePending() {return restorePending;}
    public synchronized boolean pending(dev.ronova.coremod.root.api.TargetEpoch target) {
        if(restorePending||unscopedOverflow>0||overflowScopes.containsKey(target))return true;
        var queue = subjects.get(target.subjectKey());
        return queue != null && queue.sources.values().stream().flatMap(Collection::stream)
            .anyMatch(key -> key.targetEpoch().equals(target)
                && (entry(key).candidate == Candidate.CANDIDATE || !entry(key).durable.isEmpty()));
    }
    public synchronized void clear() {
        if(!entries.isEmpty()||!retryEntries.isEmpty()||overflow!=0)version=increment(version);
        java.util.stream.Stream.concat(entries.values().stream(),retryEntries.values().stream())
            .forEach(entry->{if(entry.reservation!=null)entry.reservation.close();});
        scopeReservations.values().forEach(dev.ronova.coremod.budget.HostResourceGovernor.Reservation::close);
        entries.clear();retryEntries.clear();subjects.clear();reclaimQueue.clear();
        overflowScopes.clear();scopeReservations.clear();overflow=0;unscopedOverflow=0;restorePending=false;
    }
    private static CandidateEvidence historical(CandidateEvidence sample) {
        return new CandidateEvidence(sample.key(),sample.policyRevision(),sample.judgmentRevision(),sample.ruleRevision(),
            sample.eventRef(),sample.basisRef(),sample.family(),sample.observedAt(),sample.validUntil(),false,sample.authorizedCompliant());
    }
    private static View view(Entry e) { return new View(e.key,e.policy,e.judgment,e.candidate,e.state,e.count,e.count==Long.MAX_VALUE,e.firstSeen,e.lastSeen,e.reviews,List.copyOf(e.samples),Set.copyOf(e.durable),e.digest,e.mixedContext); }
    private static long add(long left,long right) {return left>Long.MAX_VALUE-right?Long.MAX_VALUE:left+right;}
    private static long increment(long n) { return n==Long.MAX_VALUE?n:n+1; }
}
