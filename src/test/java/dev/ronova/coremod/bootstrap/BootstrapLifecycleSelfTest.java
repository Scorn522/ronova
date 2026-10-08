package dev.ronova.coremod.bootstrap;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Failed construction, lookup-only shutdown and release invariants on production classes. */
public final class BootstrapLifecycleSelfTest {
    public static void main(String[] args) throws Exception {
        StartupRegistry<Object,Object> slots = new StartupRegistry<>();
        Object key = new Object(); int[] calls={0}, cleanups={0};
        RuntimeException original = new IllegalStateException("original Root failure");
        for (int i=0;i<3;i++) {
            try { slots.getOrCreate(key,k->{calls[0]++;throw original;},k->{cleanups[0]++;}); }
            catch (RuntimeException failure) { require(failure==original,"original failure was replaced"); }
        }
        require(calls[0]==1 && cleanups[0]==1 && slots.get(key)==null,"failed construction retried or published partial runtime");
        slots.remove(key); slots.remove(key);
        require(slots.get(key)==null && calls[0]==1,"cleanup initialized a failed slot");
        Object liveKey=new Object(), runtime=new Object();
        require(slots.get(liveKey)==null,"lookup created a runtime");
        require(slots.getOrCreate(liveKey,k->runtime,k->{})==runtime,"ready runtime was not returned");
        require(slots.remove(liveKey)==runtime && slots.remove(liveKey)==null,"release was not idempotent");
        try { slots.getOrCreate(liveKey,k->{throw new AssertionError("recreated after shutdown");},k->{}); throw new AssertionError("closed slot reopened"); }
        catch(IllegalStateException expected) {}
        noFactory("dev/ronova/coremod/mixin/MinecraftServerRuntimeMixin","ronova$beginShutdown");
        noFactory("dev/ronova/coremod/engine/JudgmentEngine","clearServer");
        noFactory("dev/ronova/coremod/engine/SpawnFirewall","clearServer");
        noFactory("dev/ronova/coremod/engine/SpawnFirewall","clearCachedServer");
        noFactory("dev/ronova/coremod/bootstrap/ServerRuntimeRegistry","getIfPresent");
        noFactory("dev/ronova/coremod/event/DominionEvents","onLevelUnload");
        noFactory("dev/ronova/coremod/observation/RecoverySourceTracker","worldUnloading");
        noFactory("dev/ronova/coremod/mixin/DimensionDataStorageRecoveryMixin","ronova$replacingSavedData");
        noFactory("dev/ronova/coremod/bootstrap/ServerRuntimeRegistry","savedDataReplacing");
        noFactory("dev/ronova/coremod/observation/RecoverySourceTracker","storageReplacing");
        System.out.println("BootstrapLifecycleSelfTest: PASS (single failed factory, original cause, rollback, lookup-only shutdown, idempotent closed slots)");
    }
    private static void noFactory(String owner,String methodName) throws Exception {
        try(InputStream in=BootstrapLifecycleSelfTest.class.getClassLoader().getResourceAsStream(owner+".class")) {
            ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);boolean found=false;
            for(MethodNode method:node.methods) if(method.name.equals(methodName)) {
                found=true;
                for(AbstractInsnNode instruction:method.instructions) if(instruction instanceof MethodInsnNode call) {
                    require(!(call.owner.equals("dev/ronova/coremod/bootstrap/ServerRuntimeRegistry")&&call.name.equals("runtime")),"shutdown calls creating runtime accessor");
                }
            }
            require(found,"missing shutdown method "+methodName);
        }
    }
    private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
