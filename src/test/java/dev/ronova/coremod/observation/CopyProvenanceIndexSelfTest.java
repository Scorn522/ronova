package dev.ronova.coremod.observation;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class CopyProvenanceIndexSelfTest {
    public static void main(String[] args) {
        Object snapshot=new String("snapshot"),equalSnapshot=new String("snapshot");
        var atomic=new java.util.concurrent.atomic.AtomicReference<Object>(snapshot);
        require(AtomicSnapshotReference.bound(atomic,snapshot),"registered atomic snapshot was not observed");
        atomic.set(equalSnapshot);
        require(!AtomicSnapshotReference.bound(atomic,snapshot),"equal replacement retained old snapshot binding");
        atomic.set(null);
        require(!AtomicSnapshotReference.bound(atomic,snapshot),"empty atomic slot retained snapshot authority");
        var subclass=new java.util.concurrent.atomic.AtomicReference<Object>(snapshot) {};
        require(AtomicSnapshotReference.unwrap(subclass)==subclass,"unregistered container subclass was treated as the registered slot");
        AtomicInteger getterCalls=new AtomicInteger();
        java.util.function.Supplier<Object> supplier=()->{getterCalls.incrementAndGet();return snapshot;};
        require(AtomicSnapshotReference.unwrap(supplier)==supplier&&getterCalls.get()==0,"discovery invoked an arbitrary getter");
        AtomicInteger live = new AtomicInteger(); Set<String> gaps = new HashSet<>();
        var index = new CopyProvenanceIndex<String>(16, bytes -> {
            live.incrementAndGet(); return () -> live.decrementAndGet();
        }, gaps::add);
        Object seed = new String("same-content"), unrelated = new String("same-content");
        Object first = new Object(), second = new Object(), third = new Object();
        require(index.track("record-a", seed, true), "seed rejected");
        require(index.resolve(unrelated).state() == CopyProvenanceIndex.State.UNOBSERVED, "equal content created provenance");
        index.copied(seed, first, 1, true);
        require(index.resolve(first).copyDepth() == 1 && index.resolve(first).evidenceVerified(), "direct observation lost");
        index.copied(first, second, 1, true);
        require(index.resolve(second).state() == CopyProvenanceIndex.State.UNOBSERVED, "direct-only policy followed a descendant");
        index.copied(first, second, Integer.MAX_VALUE, true);
        require(index.resolve(second).copyDepth() == 2 && index.resolve(second).evidenceVerified(), "extreme descendant not tracked");
        index.copied(second, third, Integer.MAX_VALUE, false);
        index.track("record-a", seed, true);
        require(!index.resolve(third).evidenceVerified(), "later proof retroactively verified an unproved edge");
        index.copied(unrelated, first, 1, false);
        require(index.resolve(first).state() == CopyProvenanceIndex.State.AMBIGUOUS, "unobserved overwrite retained old provenance");
        require(!index.track("other-owner", seed, true) && gaps.contains("record-a") && gaps.contains("other-owner"),
            "shared seed silently selected an owner");
        index.forget("record-a");
        require(live.get() == 0 && index.track("record-new-epoch", seed, true), "retired source kept its identity or resources");
        index.close(); index.close(); require(live.get() == 0, "close leaked/double-released reservations");
        var denied = new CopyProvenanceIndex<String>(1, ignored -> null, gaps::add);
        require(!denied.track("denied", new Object()) && denied.size() == 0 && gaps.contains("denied"), "budget denial lost coverage");
        denied.close();
        var resumed=new CopyProvenanceIndex<String>(2,ignored->()->{},gaps::add);
        Object stableSeed=new Object(),occupant=new Object(),lostCopy=new Object(),freshCopy=new Object();
        resumed.track("resumed-source",stableSeed,true);resumed.track("temporary",occupant,true);
        resumed.copied(stableSeed,lostCopy,1,true);
        require(gaps.contains("resumed-source")&&resumed.resolve(lostCopy).state()==CopyProvenanceIndex.State.UNOBSERVED,
            "dropped copy obtained source authority");
        resumed.forget("temporary");resumed.copied(stableSeed,freshCopy,1,true);
        require(resumed.resolve(freshCopy).evidenceVerified()
            &&resumed.resolve(lostCopy).state()==CopyProvenanceIndex.State.UNOBSERVED,
            "restored capacity failed to prove a new edge or retroactively approved the lost copy");
        resumed.close();
        var detached=new CopyProvenanceIndex<String>(8,ignored->()->{},gaps::add);
        Object oldSeed=new Object(),lateCopy=new Object(),descendant=new Object();
        detached.track("detached",oldSeed,true);detached.copied(oldSeed,lateCopy,1,true);
        clearSeedReferences(detached);
        require(detached.resolve(lateCopy).state()==CopyProvenanceIndex.State.RESOLVED
            &&detached.resolve(lateCopy).seed()==null&&detached.resolve(lateCopy).evidenceVerified()
            &&detached.hasLiveCopies("detached"),"collecting a seed erased a witnessed live successor");
        detached.copied(lateCopy,descendant,Integer.MAX_VALUE,true);
        require(detached.resolve(descendant).copyDepth()==2&&detached.resolve(descendant).evidenceVerified(),
            "detached lineage could not follow an observed descendant");
        detached.forget("detached");
        require(!detached.hasLiveCopies("detached")&&detached.resolve(descendant).state()==CopyProvenanceIndex.State.UNOBSERVED,
            "retired qualification retained detached successor authority");
        Object leftSeed=new Object(),rightSeed=new Object(),leftCopy=new Object(),rightCopy=new Object();
        detached.track("shared-key",leftSeed,true);detached.track("shared-key",rightSeed,true);
        detached.copied(leftSeed,leftCopy,1,true);detached.copied(rightSeed,rightCopy,1,true);
        clearSeedReferences(detached);
        detached.copied(leftCopy,rightCopy,Integer.MAX_VALUE,true);
        require(detached.resolve(rightCopy).state()==CopyProvenanceIndex.State.AMBIGUOUS,
            "two collected seeds were conflated through null reference equality");
        detached.close();
        AtomicInteger retained=new AtomicInteger();
        var moved=new CopyProvenanceIndex<String>(8,ignored->{retained.incrementAndGet();return ()->retained.decrementAndGet();},gaps::add);
        Object movedSeed=new Object(),priorCopy=new Object(),newCopy=new Object();
        moved.track("old-holder",movedSeed,true);moved.copied(movedSeed,priorCopy,1,true);
        moved.retireSeeds("old-holder");moved.retireSeeds("old-holder");
        require(retained.get()==1&&moved.resolve(movedSeed).state()==CopyProvenanceIndex.State.UNOBSERVED
            &&moved.resolve(priorCopy).evidenceVerified(),"seed retirement lost successors or released their reservations");
        require(moved.track("new-holder",movedSeed,true),"a reobserved seed could not obtain a distinct source revision");
        moved.copied(movedSeed,newCopy,1,true);
        require(moved.resolve(priorCopy).sourceKey().equals("old-holder")&&moved.resolve(newCopy).sourceKey().equals("new-holder"),
            "rehomed seed rewrote old successor provenance");
        moved.forget("old-holder");
        require(moved.resolve(newCopy).evidenceVerified()&&retained.get()==2,"old-source retirement invalidated new-source copies");
        moved.close();require(retained.get()==0,"rehomed lineage leaked reservations");
        AtomicInteger released = new AtomicInteger();
        var failingRelease = new CopyProvenanceIndex<String>(4, ignored -> () -> {
            released.incrementAndGet(); throw new IllegalStateException("accounting callback failed");
        }, ignored -> {});
        Object retainedSeed = new Object(), retainedCopy = new Object();
        failingRelease.track("retired", retainedSeed, true);
        failingRelease.copied(retainedSeed, retainedCopy, 1, true);
        try { failingRelease.forget("retired"); throw new AssertionError("release failure swallowed"); }
        catch (IllegalStateException expected) {
            require(released.get() == 2 && expected.getSuppressed().length == 1,
                "retirement abandoned another reservation after the first failure");
        }
        require(failingRelease.size() == 0, "retired proof retained after failed accounting");
        failingRelease.close();
        require(released.get() == 2, "close double-released retired reservations");
        System.out.println("CopyProvenanceIndexSelfTest PASS");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    /** Deterministic weak-reference collection simulation; avoids timing-dependent System.gc tests. */
    private static void clearSeedReferences(CopyProvenanceIndex<?> index) {
        try {
            var entries=CopyProvenanceIndex.class.getDeclaredField("entries");entries.setAccessible(true);
            for(Object entry:((Map<?,?>)entries.get(index)).values()) {
                var seedField=entry.getClass().getDeclaredField("seed");seedField.setAccessible(true);
                ((java.lang.ref.WeakReference<?>)seedField.get(entry)).clear();
            }
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
}
