package dev.ronova.coremod.obligation;

import java.util.*;

/** Immutable bounded checkpoint accepted only by the existing Root writer. */
public record ObligationCheckpoint(long revision, long clock, List<ObligationRegistry.View> entries,
    ObligationRegistry.Coverage coverage, CandidateRestrictions restrictions, OverflowCheckpoint overflow) {
    public ObligationCheckpoint(long revision, long clock, List<ObligationRegistry.View> entries, ObligationRegistry.Coverage coverage) {
        this(revision, clock, entries, coverage, CandidateRestrictions.EMPTY, OverflowCheckpoint.legacy(coverage.overflowCount()));
    }
    public ObligationCheckpoint(long revision, long clock, List<ObligationRegistry.View> entries,
        ObligationRegistry.Coverage coverage, CandidateRestrictions restrictions) {
        this(revision,clock,entries,coverage,restrictions,OverflowCheckpoint.legacy(coverage.overflowCount()));
    }
    public static final ObligationCheckpoint EMPTY = new ObligationCheckpoint(0,0,List.of(),new ObligationRegistry.Coverage(0,false,"Root qualifications/effect journal retained; overflow has no expanded enforcement scope"));
    public ObligationCheckpoint {
        Objects.requireNonNull(entries);Objects.requireNonNull(coverage);Objects.requireNonNull(restrictions);Objects.requireNonNull(overflow);
        if(revision<0||clock<0||entries.size()>512||coverage.overflowCount()<0
            ||coverage.needsRevalidation()!=(coverage.overflowCount()>0)||coverage.authorityAnchor()==null
            ||coverage.authorityAnchor().length()>256)throw new IllegalArgumentException("invalid checkpoint bounds");
        var keys=new HashSet<ObligationKey>();var normalized=new ArrayList<ObligationRegistry.View>();
        for(var entry:java.util.stream.Stream.concat(entries.stream(),overflow.retryEntries().stream()).toList()) {
            Objects.requireNonNull(entry);Objects.requireNonNull(entry.key());Objects.requireNonNull(entry.candidate());Objects.requireNonNull(entry.state());
            if(!keys.add(entry.key())||entry.policyRevision()<1||entry.judgmentRevision()<0||entry.count()<1
                ||entry.countSaturated()!=(entry.count()==Long.MAX_VALUE)||entry.firstSeen()<0||entry.lastSeen()<entry.firstSeen()
                ||entry.lastSeen()>clock||entry.reviewCount()<0||entry.samples().size()>4||entry.durableEvidenceRefs().size()>4
                ||entry.latestEvidenceDigest()==null||!entry.latestEvidenceDigest().matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid checkpoint entry");
            for(var evidence:entry.samples()) {
                if(!evidence.key().equals(entry.key())||evidence.policyRevision()!=entry.policyRevision()
                    ||evidence.judgmentRevision()!=entry.judgmentRevision()||evidence.observedAt()>clock)
                    throw new IllegalArgumentException("checkpoint evidence context mismatch");
            }
            for(String ref:entry.durableEvidenceRefs())if(ref==null||ref.isBlank()||ref.length()>256)throw new IllegalArgumentException("invalid durable evidence reference");
            normalized.add(new ObligationRegistry.View(entry.key(),entry.policyRevision(),entry.judgmentRevision(),entry.candidate(),entry.state(),
                entry.count(),entry.countSaturated(),entry.firstSeen(),entry.lastSeen(),entry.reviewCount(),List.copyOf(entry.samples()),
                Collections.unmodifiableSet(new TreeSet<>(entry.durableEvidenceRefs())),entry.latestEvidenceDigest(),entry.mixedContext()));
        }
        var primaryKeys=new HashSet<ObligationKey>();entries.forEach(entry->primaryKeys.add(entry.key()));
        normalized.sort(Comparator.comparing(e->e.key().toString()));
        entries=normalized.stream().filter(entry->primaryKeys.contains(entry.key())).toList();
        overflow=new OverflowCheckpoint(normalized.stream().filter(entry->!primaryKeys.contains(entry.key())).toList(),
            overflow.scopes(),overflow.unscopedCount());
        for(var scope:overflow.scopes())if(scope.lastSeen()>clock)throw new IllegalArgumentException("future overflow scope");
        if(overflow.unscopedCount()>coverage.overflowCount()
            ||(!overflow.retryEntries().isEmpty()||!overflow.scopes().isEmpty())&&coverage.overflowCount()==0)
            throw new IllegalArgumentException("overflow detail without coverage gap");
        if(revision==0&&(!entries.isEmpty()||coverage.overflowCount()!=0))throw new IllegalArgumentException("zero revision checkpoint is not empty");
        if (revision == 0 && restrictions.revision() != 0) throw new IllegalArgumentException("zero checkpoint has restrictions");
        for (var reason : restrictions.reasons()) if (reason.createdAt() > clock || reason.requestedAt() > clock)
            throw new IllegalArgumentException("future restriction receipt");
    }
    public List<ObligationRegistry.View> allEntries() {
        return java.util.stream.Stream.concat(entries.stream(),overflow.retryEntries().stream()).toList();
    }
    public boolean sameContent(ObligationCheckpoint other) {
        return sameObservationContent(other)&&restrictions.equals(other.restrictions);
    }
    /** Historical counts alone are not a live obligation for every subject in the Realm. */
    public boolean pending(dev.ronova.coremod.root.api.TargetEpoch target) {
        Objects.requireNonNull(target);
        return overflow.unscopedCount()>0||overflow.scopes().stream().anyMatch(scope->scope.target().equals(target))
            ||restrictions.reasons().stream().anyMatch(reason->reason.target().equals(target)&&reason.holds())
            ||java.util.stream.Stream.concat(entries.stream(),overflow.retryEntries().stream()).anyMatch(entry->
                entry.key().targetEpoch().equals(target)&&(entry.candidate()==ObligationRegistry.Candidate.CANDIDATE
                    ||!entry.durableEvidenceRefs().isEmpty()));
    }
    /** Caller-owned observation payload; restrictions are derived only by Root. */
    public boolean sameObservationContent(ObligationCheckpoint other) {
        return other!=null&&clock==other.clock&&entries.equals(other.entries)&&coverage.equals(other.coverage)&&overflow.equals(other.overflow);
    }
}
