package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import java.util.*;

/** Diagnostic comparison of persisted facts. Reason strings and collection order are derived metadata. */
final class LegacyMirrorFacts {
    private LegacyMirrorFacts() {}
    static boolean equivalent(ShadowDurableRootStore root, DominionLedger mirror) {
        if (mirror == null || !root.realmId().equals(mirror.realmId()) || !root.epoch().equals(mirror.epoch())) return false;
        Set<UUID> owners = new HashSet<>();
        Set<DominionLedger.Tombstone> tombstones = new HashSet<>();
        DominionSnapshot snapshot = root.snapshot();
        for (DominionDecision.Authority value : snapshot.authorities()) {
            if (value.authorized()) owners.add(value.subject().uuid());
        }
        for (DominionDecision.Judgment value : snapshot.judgments()) {
            SubjectIdentity s = value.subject();
            tombstones.add(new DominionLedger.Tombstone(s.uuid(), s.registryType(), s.runtimeClass(), s.dimension(),
                s.x(),s.y(),s.z(),value.radius(),JudgmentScope.valueOf(value.scope().name()),value.epoch().sequence(),s.player()));
        }
        Set<UUID> mirrorOwners = new HashSet<>();
        for (DominionDecision.Authority value : mirror.snapshot().authorities()) {
            if (value.authorized()) mirrorOwners.add(value.subject().uuid());
        }
        return owners.equals(mirrorOwners) && tombstones.equals(new HashSet<>(mirror.tombstones()))
            && new HashSet<LifecycleIntegrationReceipt>(root.lifecycleIntegrationReceipts())
                .equals(new HashSet<>(mirror.lifecycleIntegrationReceipts()));
    }
}
