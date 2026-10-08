package dev.ronova.coremod.runtime;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;

public final class CompletionTrackerSelfTest {
    public static void main(String[] args) {
        var subject = new SubjectKey(new RealmId(new UUID(717, 1)), new UUID(717, 2));
        var q = QualificationPreconditions.baseline(subject).judgmentTransition(new UUID(717, 3));
        var plan = TerminalOperationPlan.terminal(new UUID(717, 4), EffectRecord.hash("authority"), 2, 2, q, 1, 1);
        var scope = CompletionTracker.Scope.of(plan);
        var all = new EnumMap<CompletionTracker.Condition, CompletionTracker.Evidence>(CompletionTracker.Condition.class);
        for (var condition : CompletionTracker.Condition.values())
            all.put(condition, new CompletionTracker.Evidence(CompletionTracker.State.VERIFIED, 1, condition.name()));
        var tracker = new CompletionTracker();
        require(!tracker.sample(scope, 0, Map.of(), true, true).complete(), "call-only report became completed");
        require(!tracker.sample(scope, 1, all, true, false).complete(), "unknown effect became completed");
        for (int tick = 2; tick < 22; tick++) require(!tracker.sample(scope, tick, all, true, true).complete(), "stable window shortened");
        var complete = tracker.sample(scope, 22, all, true, true);
        require(complete.complete(), "all required evidence did not close the contract");
        // An unrelated Root commit changes plan revision, not this completion scope.
        var replan = TerminalOperationPlan.terminal(plan.commandId(), plan.authorityHash(), 2, 99, q, 1, 1);
        require(tracker.sample(CompletionTracker.Scope.of(replan), 23, all, true, true).revision() == complete.revision(),
            "unrelated Root transaction invalidated an unchanged target");
        all.remove(CompletionTracker.Condition.TRACKING);
        var invalid = tracker.sample(scope, 24, all, true, true);
        require(!invalid.complete() && invalid.revision() > complete.revision()
            && tracker.lastSatisfied().complete(), "lost required evidence did not revoke current validity and retain history");
        all.put(CompletionTracker.Condition.TRACKING, new CompletionTracker.Evidence(CompletionTracker.State.VERIFIED, 2, "tracking-repaired"));
        for (int tick = 25; tick <= 45; tick++) tracker.sample(scope, tick, all, true, true);
        require(tracker.current(45).complete(), "repaired evidence did not receive a new stable window");
        var expired = tracker.current(56);
        require(!expired.complete() && expired.fenced(), "stopped observer left completion green");
        require(!tracker.sample(scope, 57, all, true, true).complete(), "late refresh reused an expired window");
        long version = tracker.current(57).revision();
        for (int i = 0; i < 100; i++) require(!tracker.sample(scope, 57, all, true, true).complete(), "duplicate samples created elapsed time");
        require(tracker.current(57).revision() == version, "duplicate evidence churned the revision");
        var another = new CompletionTracker.Scope(scope.commandId(), scope.authorityHash(), q.judgmentTransition(new UUID(717, 5)), 1, 1);
        require(!tracker.sample(another, 58, all, true, true).complete(), "new qualification reused old completion");
        require(tracker.sample(another, 59, all, false, true).fenced(), "revoked authority remained usable");
        System.out.println("CompletionTrackerSelfTest: PASS (nine conditions, unknown effects, stability, duplicates, scoped invalidation, observer expiry, history)");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
