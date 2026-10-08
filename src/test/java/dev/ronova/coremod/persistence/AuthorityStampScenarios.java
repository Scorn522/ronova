package dev.ronova.coremod.persistence;

import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;

final class AuthorityStampScenarios {
    static void verify() throws Exception {
        Path dir = Files.createTempDirectory("ronova-authority-stamp-").toAbsolutePath().normalize();
        try {
            var store = LocalDominionStore.openWorld(dir); UUID actor = new UUID(61, 10);
            var unbound = store.authorityStamp(actor);
            require(unbound.settled() && !unbound.owner() && unbound.ownerAuthorizationRevision() == 0, "fresh authority stamp invalid");
            var grant = new DominionCommand.BindOwner(actor); store.commit(grant);
            var first = store.authorityStamp(actor);
            store.commit(new DominionCommand.BindOwner(new UUID(61, 11)));
            require(first.sameAuthority(store.authorityStamp(actor)), "unrelated owner changed scoped authorization");
            var revoke = new DominionCommand.UnbindOwner(actor);
            var backend = DurableRootFileBackend.at(dir.resolve("ronova/durable-root-v1.bin"));
            // Use the actual owned backend to exercise acknowledged-head isolation.
            store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(store.commit(revoke).unknown(), "revocation ACK-loss fixture failed");
            var pending = store.authorityStamp(actor);
            require(!pending.settled() && pending.owner() && pending.ownerAuthorizationRevision() == first.ownerAuthorizationRevision(),
                "pending writer exposed unacknowledged Owner qualification");
            store.recover(revoke.commandId());
            store.commit(new DominionCommand.BindOwner(actor));
            var again = store.authorityStamp(actor);
            require(again.owner() && again.ownerAuthorizationRevision() > first.ownerAuthorizationRevision()
                && !first.sameAuthority(again), "Owner revoke/regrant ABA was invisible");
            var rootBytes = Files.readAllBytes(backend.target());
            var reopened = LocalDominionStore.openWorld(dir);
            require(again.equals(reopened.authorityStamp(actor)) && Arrays.equals(rootBytes, Files.readAllBytes(backend.target())),
                "derived owner revisions changed history or Root bytes on restart");
            System.out.println("AuthorityStampScenarios: PASS (acknowledged authority, Owner ABA, unrelated scope, original journal restart)");
        } finally {
            try (var paths = Files.walk(dir)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (!p.toAbsolutePath().normalize().startsWith(dir)) throw new IllegalStateException("unsafe test cleanup");
                Files.deleteIfExists(p);
            }}
        }
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
