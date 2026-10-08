package dev.ronova.coremod.capability;

import dev.ronova.coremod.contract.Capability;

/** One isolated server capability implementation. Modules never register their own tick loop. */
public interface ServerCapabilityModule {
    Capability capability();

    void repair(CapabilityContext context);
}
