package dev.ronova.coremod.observation;

import dev.ronova.coremod.root.api.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class RecoveryLocationResolverSelfTest {
    private static class Data { private Map<UUID,Object> records = new HashMap<>(); }
    private static final class Child extends Data {}
    private static final class Entry {
        private Object snapshot;
        Entry(Object value) { snapshot = value; }
        public Object getSnapshot() { throw new AssertionError("foreign getter called"); }
    }
    private static final class Manager { private static Map<UUID,Object> records = new HashMap<>(); }
    public static void main(String[] args) {
        UUID subject = UUID.randomUUID(); Object snapshot = new Object();
        Data owner = new Child(); var record = new Entry(new AtomicReference<>(snapshot)); owner.records.put(subject, record);
        var loaded = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA,
            "test:world", "key", Data.class.getName(), "records", Entry.class.getName(), "snapshot");
        var result = RecoveryLocationResolver.loaded(owner, loaded, subject, () -> true);
        require(result.state() == RecoveryLocationResolver.State.PRESENT && result.view().snapshot() == snapshot
            && result.view().record() == record && result.view().owner() == owner, "inherited live location not resolved precisely");
        require(RecoveryLocationResolver.loaded(owner, loaded, subject, () -> false).state() == RecoveryLocationResolver.State.BUDGET_EXHAUSTED,
            "budget denial was presented as absence");
        record.snapshot = new AtomicReference<>();
        require(RecoveryLocationResolver.loaded(owner, loaded, subject, () -> true).state() == RecoveryLocationResolver.State.SNAPSHOT_EMPTY,
            "empty atomic cell was not distinguished from an absent entry");
        owner.records.remove(subject);
        require(RecoveryLocationResolver.loaded(owner, loaded, subject, () -> true).state() == RecoveryLocationResolver.State.ENTRY_ABSENT,
            "missing entry was not observed");
        owner.records.put(subject, null);
        require(RecoveryLocationResolver.loaded(owner, loaded, subject, () -> true).state() == RecoveryLocationResolver.State.SNAPSHOT_EMPTY,
            "null placeholder was reported as an absent entry");
        owner.records = new HashMap<>() { @Override public Object get(Object key) { throw new AssertionError("custom map called"); } };
        require(RecoveryLocationResolver.loaded(owner, loaded, subject, () -> true).state() == RecoveryLocationResolver.State.UNAVAILABLE,
            "unknown map semantics were accepted");
        var wrongOwner = new RecoverySourceLocation(loaded.scope(), loaded.dimension(), loaded.storageName(),
            "does.not.Exist", "records", "", "");
        require(RecoveryLocationResolver.loaded(owner, wrongOwner, subject, () -> true).state() == RecoveryLocationResolver.State.UNAVAILABLE,
            "unobserved named owner was guessed or loaded");
        var statik = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER, "", "",
            Manager.class.getName(), "records", "", "");
        Manager.records.put(subject, snapshot);
        try {
            require(RecoveryLocationResolver.fromObservedClass(Manager.class, statik, subject, () -> true).view().snapshot() == snapshot,
                "observed initialized static holder not resolved");
            require(RecoveryLocationResolver.loaded(owner, statik, subject, () -> true).state() == RecoveryLocationResolver.State.UNAVAILABLE,
                "location crossed loaded/static scope");
        } finally { Manager.records.clear(); }
        verifiesObservedLoaderIdentity();
        System.out.println("RecoveryLocationResolverSelfTest PASS");
    }
    private static void verifiesObservedLoaderIdentity() {
        var image=new org.objectweb.asm.ClassWriter(0);
        image.visit(org.objectweb.asm.Opcodes.V17,org.objectweb.asm.Opcodes.ACC_PUBLIC|org.objectweb.asm.Opcodes.ACC_SUPER,
            "fixture/DuplicateManager",null,"java/lang/Object",null);
        image.visitField(org.objectweb.asm.Opcodes.ACC_PUBLIC|org.objectweb.asm.Opcodes.ACC_STATIC,"records","Ljava/util/Map;",null,null).visitEnd();
        image.visitEnd();
        var first=new FixtureLoader().define(image.toByteArray());
        var second=new FixtureLoader().define(image.toByteArray());
        UUID subject=UUID.randomUUID();Object left=new Object(),right=new Object();
        var leftMap=new HashMap<UUID,Object>();leftMap.put(subject,left);
        var rightMap=new HashMap<UUID,Object>();rightMap.put(subject,right);
        try {
            first.getField("records").set(null,leftMap);second.getField("records").set(null,rightMap);
            var hint=new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER,"","",first.getName(),"records","","");
            var a=RecoveryLocationResolver.fromObservedClass(first,hint,subject,()->true);
            var b=RecoveryLocationResolver.fromObservedClass(second,hint,subject,()->true);
            require(first!=second&&first.getName().equals(second.getName())&&a.view().container()==leftMap&&a.view().snapshot()==left
                &&b.view().container()==rightMap&&b.view().snapshot()==right,"binary-name hint selected another loader's holder");
        } catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static final class FixtureLoader extends ClassLoader {
        Class<?> define(byte[] image){return defineClass(null,image,0,image.length);}
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
