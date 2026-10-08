package dev.ronova.coremod.runtime;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.engine.spawn.AdmissionPass;
import dev.ronova.coremod.util.WeakIdentityMap;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class DeferredAuthorityTaskSelfTest {
    public static void main(String[] args) throws Exception {
        SubjectKey key = new SubjectKey(new RealmId(new UUID(61, 1)), new UUID(61, 2));
        DominionEpoch epoch = DominionEpoch.LOCAL_INITIAL;
        SubjectQualification qualified = QualificationPreconditions.baseline(key).rebind(new UUID(61, 3));
        AuthorityStamp first = new AuthorityStamp(key, qualified, 1, true, 7, true, epoch);
        AtomicInteger effects = new AtomicInteger();
        var task = new DeferredAuthorityTask(first, 100, 20);
        require(task.run(first, true, 110, effects::incrementAndGet) == DeferredAuthorityTask.Result.EXECUTED, "current work refused");
        require(task.run(first, true, 111, effects::incrementAndGet) == DeferredAuthorityTask.Result.ALREADY_CONSUMED, "task replayed");
        for (AuthorityStamp stale : List.of(
            new AuthorityStamp(key, qualified.transition(true), 1, true, 7, true, epoch),
            new AuthorityStamp(key, qualified, 2, true, 7, true, epoch),
            new AuthorityStamp(key, qualified, 1, true, 9, true, epoch),
            new AuthorityStamp(key, qualified, 1, true, 7, false, epoch))) {
            require(new DeferredAuthorityTask(first, 100, 20).run(stale, true, 110, effects::incrementAndGet)
                == DeferredAuthorityTask.Result.STALE, "stale/ABA/pending authority executed");
        }
        require(new DeferredAuthorityTask(first, 100, 20).run(first, false, 110, effects::incrementAndGet)
            == DeferredAuthorityTask.Result.CONTEXT_LOST, "old connection/runtime executed");
        require(new DeferredAuthorityTask(first, 100, 20).run(first, true, 121, effects::incrementAndGet)
            == DeferredAuthorityTask.Result.EXPIRED, "expired work executed");
        AuthorityStamp unrelatedCommit = new AuthorityStamp(key, qualified, 1, true, 7, true, epoch.nextSequence());
        require(new DeferredAuthorityTask(first, 100, 20).run(unrelatedCommit, true, 110, effects::incrementAndGet)
            == DeferredAuthorityTask.Result.EXECUTED, "unrelated subject commit revoked this task");
        UUID nonce = qualified.revisions().activeIncarnation().nonce();
        AdmissionPass pass = new AdmissionPass(first, nonce, epoch);
        require(pass.current(first, nonce, epoch) && !pass.current(first, UUID.randomUUID(), epoch)
            && !pass.current(unrelatedCommit, nonce, epoch) && !pass.current(first, nonce, epoch.nextSequence()),
            "admission pass crossed physical identity or a stale projection");
        WeakIdentityMap<Object, Integer> map = new WeakIdentityMap<>();
        Object a = new String("same"), b = new String("same"); map.put(a, 1); map.put(b, 2);
        require(map.get(a) == 1 && map.get(b) == 2, "cache used logical equals as physical identity");
        map.removeIf(value -> value == a); require(map.get(a) == null && map.get(b) == 2, "scoped removal removed another object");
        var concurrent = new DeferredAuthorityTask(first, 0, 100);
        var pool = Executors.newFixedThreadPool(2); AtomicInteger runs = new AtomicInteger();
        try {
            var f1 = pool.submit(() -> concurrent.run(first, true, 1, runs::incrementAndGet));
            var f2 = pool.submit(() -> concurrent.run(first, true, 1, runs::incrementAndGet));
            f1.get(5, TimeUnit.SECONDS); f2.get(5, TimeUnit.SECONDS);
            require(runs.get() == 1, "concurrent work claimed one token twice");
        } finally { pool.shutdownNow(); }
        require(effects.get() == 2, "denied tasks produced side effects");
        try (var bytes = DeferredAuthorityTaskSelfTest.class.getResourceAsStream("/dev/ronova/coremod/mixin/ServerLevelTickAdmissionMixin.class")) {
            var node = new org.objectweb.asm.tree.ClassNode(); new org.objectweb.asm.ClassReader(Objects.requireNonNull(bytes)).accept(node, 0);
            int gates = 0;
            for (var method : node.methods) for (var instruction : method.instructions) {
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                    && call.owner.equals("dev/ronova/coremod/engine/SpawnFirewall") && call.name.equals("reject")) gates++;
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field && field.desc.equals("I"))
                    throw new AssertionError("tick guard is conditional on a first-tick counter");
            }
            require(gates == 2, "normal and passenger ticks do not both check current qualification");
        }
        try (var bytes = DeferredAuthorityTaskSelfTest.class.getResourceAsStream("/dev/ronova/coremod/mixin/PersistentEntityAdmissionMixin.class")) {
            var node = new org.objectweb.asm.tree.ClassNode(); new org.objectweb.asm.ClassReader(Objects.requireNonNull(bytes)).accept(node, 0);
            boolean rawScheduling = false;
            for (var method : node.methods) if (method.name.equals("ronova$rejectTicking")) for (var instruction : method.instructions) {
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field && field.name.equals("ENTITY_MANAGER")) rawScheduling = true;
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field && field.name.equals("FIRST_TICK"))
                    throw new AssertionError("held raw scheduling can never resume after rebind");
            }
            require(rawScheduling, "entity manager does not retain raw ticking membership");
        }
        System.out.println("DeferredAuthorityTaskSelfTest: PASS (qualification/Owner ABA/policy/expiry/context, one-use race, independent epochs, physical pass cache)");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
