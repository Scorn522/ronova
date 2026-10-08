package dev.ronova.coremod.agent;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.zip.ZipInputStream;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Verifies complete platform bodies plus paired execution exits, including exceptional control flow. */
public final class JdkTaskBoundarySelfTest {
    public static void main(String[] args) throws Exception {
        for(String name:List.of("java/util/concurrent/FutureTask","java/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask",
            "java/util/concurrent/ForkJoinTask","java/util/concurrent/ThreadPoolExecutor",
            "java/util/concurrent/ScheduledThreadPoolExecutor","java/util/concurrent/ForkJoinPool",
            "java/util/concurrent/ThreadPoolExecutor$CallerRunsPolicy")) {
            byte[] original;
            try(InputStream in=ClassLoader.getPlatformClassLoader().getResourceAsStream(name+".class")){original=in.readAllBytes();}
            byte[] patched=JdkTaskBoundaryContract.transform(name,original);
            require(patched!=null&&JdkTaskBoundaryContract.verify(name,patched),"platform task patch failed: "+name);
            require(JdkTaskBoundaryContract.transform(name,patched)==null,"task instrumentation was not idempotent");
            var reconstructed=new ClassNode();new ClassReader(original).accept(reconstructed,0);
            reconstructed.methods.forEach(m->m.access&=~Opcodes.ACC_DEPRECATED);
            Collections.reverse(reconstructed.methods);
            var vmWriter=new ClassWriter(0);reconstructed.accept(vmWriter);
            require(JdkTaskBoundaryContract.transform(name,vmWriter.toByteArray())!=null,"VM metadata reconstruction rejected: "+name);
            var node=new ClassNode();new ClassReader(patched).accept(node,0);
            MethodNode instrumented=node.methods.stream().filter(m->Arrays.stream(m.instructions.toArray())
                .anyMatch(i->i instanceof MethodInsnNode c&&c.owner.equals("dev/ronova/coremod/agent/BootstrapTaskHooks"))).findFirst().orElseThrow();
            AbstractInsnNode hook=Arrays.stream(instrumented.instructions.toArray()).filter(i->i instanceof MethodInsnNode c
                &&c.owner.equals("dev/ronova/coremod/agent/BootstrapTaskHooks")).findFirst().orElseThrow();
            instrumented.instructions.remove(hook);
            var changed=new ClassWriter(0);node.accept(changed);
            require(!JdkTaskBoundaryContract.verify(name,changed.toByteArray()),"missing task boundary accepted");
        }
        String future="java/util/concurrent/FutureTask";byte[] raw;
        try(InputStream in=ClassLoader.getPlatformClassLoader().getResourceAsStream(future+".class")){raw=in.readAllBytes();}
        var node=new ClassNode();new ClassReader(raw).accept(node,0);
        var cancel=node.methods.stream().filter(m->m.name.equals("cancel")).findFirst().orElseThrow();
        cancel.instructions.clear();cancel.tryCatchBlocks.clear();if(cancel.localVariables!=null)cancel.localVariables.clear();
        cancel.instructions.add(new InsnNode(Opcodes.ICONST_1));cancel.instructions.add(new InsnNode(Opcodes.IRETURN));
        var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);
        require(JdkTaskBoundaryContract.transform(future,writer.toByteArray())==null,"unknown cancellation body was silently blessed");
        int[] events={0};BootstrapTaskHooks.install(event->events[0]++);
        Object token=BootstrapTaskHooks.enter(new FutureTask<>(()->null));BootstrapTaskHooks.submitted(new Object(),new Object());BootstrapTaskHooks.exit(token);
        require(token==null&&events[0]==0&&BootstrapTaskHooks.currentTask()==null,"direct observer invocation forged JDK execution");
        verifyPackagedTaskHookBundle();
        // Keep the pure gate concurrency regression inside an already-registered project self-test so
        // it cannot silently compile without ever executing. No build-script mutation is required.
        BootstrapTaskGateSelfTest.main(new String[0]);
        System.out.println("JdkTaskBoundarySelfTest PASS (platform bodies, seven adapters, idempotence, tamper rejection, no forged entry, gate regression)");
    }
    private static void verifyPackagedTaskHookBundle() throws Exception {
        try(InputStream resource=JdkTaskBoundarySelfTest.class.getResourceAsStream("/META-INF/ronova/task-hook-support.jar")) {
            require(resource!=null,"packaged task-hook-support.jar missing from production artifact");
            boolean hooks=false;
            try(ZipInputStream nested=new ZipInputStream(resource)) {
                for(var entry=nested.getNextEntry();entry!=null;entry=nested.getNextEntry())
                    if(entry.getName().equals("dev/ronova/coremod/agent/BootstrapTaskHooks.class")){hooks=true;break;}
            }
            require(hooks,"packaged task-hook-support.jar missing BootstrapTaskHooks.class");
        }
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
