package dev.ronova.coremod.obligation;

import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.root.api.*;
import java.util.*;
import java.util.function.Predicate;

/** Root-derived, bounded reasons for reversible admission holds. These are not judgments. */
public record CandidateRestrictions(long revision, long retiredCount, String retiredDigest, List<Reason> reasons) {
    public static final int MAX_REASONS = 512;
    public static final CandidateRestrictions EMPTY = new CandidateRestrictions(0, 0, EffectRecord.hash("empty-restrictions/v1"), List.of());
    public enum Decision { UNRESOLVED, PROVEN_UNRELATED }
    public enum Release { HELD, RELEASE_PENDING, RELEASED }
    public record Reason(String id, TargetEpoch target, UUID physicalNonce, long policyRevision,
                         long revision, Decision decision, Release release, long createdAt,
                         long requestedAt, long notBefore, long deadline, String evidenceRef) {
        public Reason {
            Objects.requireNonNull(target); // null nonce denotes the Root-controlled stale-incarnation outlet for this target epoch.
            Objects.requireNonNull(decision); Objects.requireNonNull(release);
            if (!id.equals(reasonId(target, physicalNonce)) || policyRevision < 1 || revision < 1 || createdAt < 0
                || !evidenceRef.matches("[0-9a-f]{64}") || requestedAt < 0 || notBefore < requestedAt || deadline < notBefore
                || decision == Decision.UNRESOLVED && release != Release.HELD
                || release == Release.RELEASE_PENDING && (decision != Decision.PROVEN_UNRELATED || deadline - requestedAt > 100)
                || release != Release.HELD && requestedAt < createdAt)
                throw new IllegalArgumentException("invalid candidate restriction reason");
        }
        public boolean holds() { return release != Release.RELEASED; }
    }
    public CandidateRestrictions {
        Objects.requireNonNull(reasons);
        if (revision < 0 || retiredCount < 0 || !retiredDigest.matches("[0-9a-f]{64}") || reasons.size() > MAX_REASONS)
            throw new IllegalArgumentException("invalid restriction snapshot");
        var ids = new HashSet<String>();
        for (var reason : reasons) if (!ids.add(reason.id()) || reason.revision() > revision)
            throw new IllegalArgumentException("duplicate/future restriction reason");
        reasons = reasons.stream().sorted(Comparator.comparing(Reason::id)).toList();
        if (revision == 0 && (retiredCount != 0 || !reasons.isEmpty())) throw new IllegalArgumentException("nonempty zero revision restrictions");
    }
    public static String reasonId(TargetEpoch target, UUID physicalNonce) {
        return EffectRecord.hash("admission-hold/v1/" + target.subjectKey().canonical() + "/" + target.generation() + "/" + (physicalNonce==null?"stale-incarnations":physicalNonce));
    }
    public boolean holds(SubjectQualification current, UUID physicalNonce) {
        if (current==null || current.terminal() || current.revisions()==null || current.revisions().activeIncarnation()==null
            || current.revisions().activeIncarnation().nonce().equals(physicalNonce)) return false;
        TargetEpoch target=current.targetEpoch();
        String id = reasonId(target, physicalNonce);
        return reasons.stream().anyMatch(r -> r.holds() && (r.id().equals(id) || r.physicalNonce()==null && r.target().equals(target)));
    }

    /** Inputs are acknowledged Root facts. Reported names, counts, scores or metadata cannot establish a hold. */
    public CandidateRestrictions reconcile(List<ObligationRegistry.View> observations,
        Map<SubjectKey, SubjectQualification> qualifications, Predicate<SubjectKey> protectedSubject,
        long policyRevision, long clock) {
        var next = new TreeMap<String, Reason>(); reasons.forEach(r -> next.put(r.id(), r));
        long nextRevision = revision, retired = retiredCount; String digest = retiredDigest;
        for (var old : reasons) {
            var current = qualifications.get(old.target().subjectKey());
            // A canonical transition ends the old scope immediately; no candidate debounce delays it.
            boolean superseded = current != null && current.targetEpoch().generation() > old.target().generation();
            boolean protectedNow = protectedSubject.test(old.target().subjectKey());
            if (old.holds() && (superseded || protectedNow)) {
                nextRevision = Math.addExact(nextRevision, 1);
                next.put(old.id(), new Reason(old.id(), old.target(), old.physicalNonce(), old.policyRevision(), nextRevision,
                    Decision.PROVEN_UNRELATED, Release.RELEASED, old.createdAt(), clock, clock, clock,
                    EffectRecord.hash("root-qualification-transition/" + current + "/protected=" + protectedNow)));
            } else if (old.release() == Release.RELEASE_PENDING && clock >= old.deadline()) {
                nextRevision = Math.addExact(nextRevision, 1);
                next.put(old.id(), new Reason(old.id(), old.target(), old.physicalNonce(), old.policyRevision(), nextRevision,
                    old.decision(), Release.RELEASED, old.createdAt(), old.requestedAt(), old.notBefore(), old.deadline(), old.evidenceRef()));
            }
        }
        for (var view : observations) {
            if (view.candidate()!=ObligationRegistry.Candidate.CANDIDATE || view.state()!=ObligationRegistry.State.UNRESOLVED
                || view.key().source() != ObligationKey.Source.ADMISSION_GATE || view.key().edge() != ObligationKey.Edge.ADMISSION
                || view.key().result() != ObligationKey.Result.CURRENT_QUALIFICATION || view.policyRevision() != policyRevision)
                continue;
            var q = qualifications.get(view.key().targetEpoch().subjectKey());
            if (q == null || q.terminal() || !q.targetEpoch().equals(view.key().targetEpoch()) || q.revisions() == null
                || q.revisions().activeIncarnation() == null || protectedSubject.test(q.subjectKey())) continue;
            for (var evidence : view.samples()) {
                if (!evidence.observerVerified() || evidence.authorizedCompliant() || evidence.family() != CandidateEvidence.Family.STALE_QUALIFICATION
                    || evidence.observedAt() > clock || evidence.validUntil() < clock || evidence.ruleRevision() != 1
                    || evidence.judgmentRevision() != q.revisions().judgmentRevision()) continue;
                UUID nonce;
                try {
                    String[] event = evidence.eventRef().split("/", -1);
                    if (event.length > 2 || event.length == 2 && !event[1].equals(Long.toString(evidence.observedAt()))) continue;
                    nonce = UUID.fromString(event[0]);
                } catch (IllegalArgumentException invalid) { continue; }
                if (nonce.equals(q.revisions().activeIncarnation().nonce())) continue;
                var summary = next.get(reasonId(q.targetEpoch(),null));
                if (summary != null && (summary.holds() || evidence.observedAt() <= summary.requestedAt())) continue;
                String id = reasonId(q.targetEpoch(), nonce);
                var old = next.get(id);
                if (old != null && old.release() == Release.HELD) continue;
                // Repeated/expired observations cannot extend a proven-unrelated release window.
                if (old != null && evidence.observedAt() <= old.requestedAt()) continue;
                if (next.size() == MAX_REASONS && old == null) {
                    var victim = next.values().stream().filter(r -> r.release() == Release.RELEASED).findFirst().orElse(null);
                    if (victim == null) continue; // Independent Root qualification gate still denies stale submissions.
                    next.remove(victim.id()); retired = Math.addExact(retired, 1);
                    digest = EffectRecord.hash(digest + "/" + victim.id() + "/" + victim.revision());
                }
                nextRevision = Math.addExact(nextRevision, 1);
                next.put(id, new Reason(id, q.targetEpoch(), nonce, policyRevision, nextRevision, Decision.UNRESOLVED,
                    Release.HELD, old == null ? clock : old.createdAt(), 0, 0, 0,
                    EffectRecord.hash("root-binding-mismatch/v1/" + q + "/" + evidence.eventRef())));
            }
        }
        // Handoff occurs in one immutable checkpoint publication. Never evict a pending-release reason into an unresolved summary.
        var groups = new LinkedHashMap<TargetEpoch,List<Reason>>();
        for (var reason : next.values()) if (reason.physicalNonce()!=null && reason.release()==Release.HELD)
            groups.computeIfAbsent(reason.target(),ignored->new ArrayList<>()).add(reason);
        for (var group : groups.entrySet()) if (group.getValue().size()>8) {
            long first=clock;
            for (var reason : group.getValue()) {
                first=Math.min(first,reason.createdAt()); next.remove(reason.id()); retired=Math.addExact(retired,1);
                digest=EffectRecord.hash(digest+"/handoff/"+reason.id()+"/"+reason.revision());
            }
            nextRevision=Math.addExact(nextRevision,1);
            String id=reasonId(group.getKey(),null);
            next.put(id,new Reason(id,group.getKey(),null,policyRevision,nextRevision,Decision.UNRESOLVED,Release.HELD,
                first,0,0,0,EffectRecord.hash("root-stale-incarnation-outlet/v1/"+group.getKey()+"/"+digest)));
        }
        return new CandidateRestrictions(nextRevision, retired, digest, List.copyOf(next.values()));
    }

    /** A scope-specific verified conclusion starts one bounded window; callers must authenticate that proof at Root. */
    public CandidateRestrictions requestRelease(String id, long expectedRevision, long clock, String proof) {
        var next = new ArrayList<Reason>(reasons); boolean changed = false;
        long updated = Math.addExact(revision, 1);
        for (int i=0; i<next.size(); i++) {
            var r = next.get(i);
            if (!r.id().equals(id)) continue;
            if (r.physicalNonce()==null) throw new IllegalArgumentException("summary release requires a whole-scope Root qualification transition");
            if (r.revision() != expectedRevision) throw new IllegalArgumentException("stale restriction release");
            if (r.release() != Release.HELD) return this;
            next.set(i, new Reason(r.id(), r.target(), r.physicalNonce(), r.policyRevision(), updated,
                Decision.PROVEN_UNRELATED, Release.RELEASE_PENDING, r.createdAt(), clock,
                Math.addExact(clock, 20), Math.addExact(clock, 100), proof));
            changed = true;
        }
        if (!changed) throw new IllegalArgumentException("unknown restriction reason");
        return new CandidateRestrictions(updated, retiredCount, retiredDigest, next);
    }
}
