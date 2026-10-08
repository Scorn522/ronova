package dev.ronova.coremod.contract;

import dev.ronova.coremod.engine.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;
import org.objectweb.asm.*;

public final class ClassLineageResolverSelfTest {
    public static void main(String[] args) {
        class Loader extends ClassLoader {
            Class<?> type(String name, String parent) {
                ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, parent, null); writer.visitEnd();
                byte[] bytes = writer.toByteArray(); return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            }
        }
        var first = new Loader(); var second = new Loader();
        Class<?> source = first.type("fixture/Producer", "java/lang/Object");
        Class<?> unrelated = second.type("fixture/Producer", "java/lang/Object");
        Class<?> child = first.type("fixture/Child", "fixture/Producer");
        var realm = new RealmId(new UUID(717, 10)); var key = new SubjectKey(realm, new UUID(717, 11));
        var q = QualificationPreconditions.baseline(key).judgmentTransition(new UUID(717, 12));
        var resolver = new ClassLineageResolver(1, null);
        Set<SubjectQualification> live = new HashSet<>(Set.of(q));
        var mark = new JudgmentMark(key.subjectId(), "fixture:source", "minecraft:overworld", 0, 0, 0, 10, JudgmentScope.LOCAL_LINEAGE);
        require(resolver.remember(q, mark, source, live::contains), "bound origin not retained");
        var nearby = subject("minecraft:overworld", 3);
        require(resolver.resolve(source, nearby, live::contains) != null, "same actual class lost lineage");
        require(resolver.resolve(child, nearby, live::contains) != null, "proved inheritance lost lineage");
        require(resolver.resolve(unrelated, nearby, live::contains) == null, "same name in another loader became lineage");
        require(resolver.resolve(Object.class, nearby, live::contains) == null, "parent/shared helper automatically became hostile");
        require(resolver.resolve(source, subject("minecraft:overworld", 11), live::contains) == null
            && resolver.resolve(source, subject("minecraft:the_nether", 3), live::contains) == null, "local scope widened");
        live.clear();
        require(resolver.resolve(child, nearby, live::contains) == null, "revoked qualification retained class lineage");
        live.add(q);
        var next = QualificationPreconditions.baseline(new SubjectKey(realm, new UUID(717, 13))).judgmentTransition(new UUID(717, 14));
        live.add(next);
        var nextMark = new JudgmentMark(next.subjectKey().subjectId(), "fixture:other", "minecraft:overworld", 0, 0, 0, 0, JudgmentScope.GLOBAL_LINEAGE);
        require(!resolver.remember(next, nextMark, unrelated, live::contains) && resolver.unresolvedRegistrations() == 1
            && resolver.resolve(source, nearby, live::contains) != null, "pressure evicted established origin or invented new proof");
        live.remove(q);
        require(resolver.remember(next, nextMark, unrelated, live::contains), "invalid origin did not release its observation slot");
        require(new ClassLineageResolver(1, null).resolve(unrelated, nearby, live::contains) == null, "restart invented loader provenance");
        resolver.clear(); require(resolver.resolve(unrelated, nearby, live::contains) == null, "shutdown retained class provenance");
        System.out.println("ClassLineageResolverSelfTest: PASS (defining-loader identity, inheritance, local scope, revocation, bounded retention, restart)");
    }
    private static SubjectIdentity subject(String dimension, double x) {
        return new SubjectIdentity(UUID.randomUUID(), "fixture:unrelated-registry", "fixture.Producer", dimension, x, 0, 0, false);
    }
    private static void require(boolean condition, String detail) { if (!condition) throw new AssertionError(detail); }
}
