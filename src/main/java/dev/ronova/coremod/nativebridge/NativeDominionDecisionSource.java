package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionRequest;
import dev.ronova.coremod.core.DominionDecisionSource;
import dev.ronova.coremod.core.LocalDominionDecisionSource;

/** Native-first R3 decision source with an observable Java fallback. */
public final class NativeDominionDecisionSource implements DominionDecisionSource {
    public static final NativeDominionDecisionSource INSTANCE =
        new NativeDominionDecisionSource();

    private NativeDominionDecisionSource() {}

    @Override
    public DominionDecision decide(DominionRequest request) {
        DominionDecision decision = NativeRootBridge.decide(request);
        if (decision != null) {
            return decision;
        }
        NativeRootBridge.recordDegradedDecision();
        return LocalDominionDecisionSource.INSTANCE.decide(request);
    }
}
