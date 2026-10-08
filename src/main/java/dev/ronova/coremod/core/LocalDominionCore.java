package dev.ronova.coremod.core;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionRequest;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Single process-wide decision entry point; adapters never own final policy. */
public final class LocalDominionCore {
    private static final AtomicReference<DominionDecisionSource> SOURCE =
        new AtomicReference<>(LocalDominionDecisionSource.INSTANCE);

    private LocalDominionCore() {}

    public static DominionDecision decide(DominionRequest request) {
        DominionDecision proposal = Objects.requireNonNull(
            SOURCE.get().decide(Objects.requireNonNull(request, "request")),
            "decision source returned null"
        );
        if (request instanceof DominionRequest.Judgment judgment) {
            if (!(proposal instanceof DominionDecision.Judgment decision)
                || !decision.subject().equals(judgment.subject()) || !decision.epoch().equals(judgment.epoch())) {
                throw new IllegalStateException("judgment proposal changed the requested subject or epoch");
            }
            // Legacy/native providers may propose a broader scope; Java Root policy owns the request ceiling.
            DominionDecision.LineageScope ceiling = judgment.subject().player()
                ? DominionDecision.LineageScope.IDENTITY : judgment.requestedScope();
            DominionDecision.LineageScope scope = decision.scope().ordinal() > ceiling.ordinal() ? ceiling : decision.scope();
            return new DominionDecision.Judgment(judgment.subject(), judgment.epoch(),
                decision.committed() && judgment.actorAuthorized() && !judgment.alreadyTerminal(), scope,
                scope == DominionDecision.LineageScope.IDENTITY ? judgment.requestedRadius() : 0D, decision.reason());
        }
        return proposal;
    }

    /** Explicit cutover seam shared by the local fallback and Native root. */
    public static DominionDecisionSource install(DominionDecisionSource source) {
        return SOURCE.getAndSet(Objects.requireNonNull(source, "source"));
    }

    public static void resetLocal() {
        SOURCE.set(LocalDominionDecisionSource.INSTANCE);
    }
}
