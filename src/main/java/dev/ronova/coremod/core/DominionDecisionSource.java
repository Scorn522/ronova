package dev.ronova.coremod.core;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionRequest;

/** Replaceable decision seam used by Local, Native and External Root stages. */
@FunctionalInterface
public interface DominionDecisionSource {
    DominionDecision decide(DominionRequest request);
}
