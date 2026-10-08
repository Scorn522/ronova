package dev.ronova.coremod.root.api;

import java.util.Objects;

/** A conditional Root plan for one observed source location, not a class-wide or new-subject Judgment. */
public record RecoverySourcePlan(TerminalOperationPlan terminal, String sourceLocator,
                                 int maximumCopyDepth, String contract) {
    public static final String CONTRACT = "ronova:observed-nbt-source-qualification/v1";
    public RecoverySourcePlan {
        Objects.requireNonNull(terminal);
        sourceLocator = EffectRecord.checkedHash(sourceLocator);
        if ((maximumCopyDepth != 1 && maximumCopyDepth != Integer.MAX_VALUE) || !CONTRACT.equals(contract))
            throw new IllegalArgumentException("unregistered recovery source contract");
    }
    public EffectKey effectKey() {
        return new EffectKey(terminal.commandId(), terminal.qualification().targetEpoch(),
            EffectRecord.SOURCE_QUALIFICATION, "source/" + sourceLocator);
    }
    public boolean coversDepth(int depth) { return depth >= 0 && depth <= maximumCopyDepth; }
}
