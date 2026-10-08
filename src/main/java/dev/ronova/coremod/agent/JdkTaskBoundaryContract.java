package dev.ronova.coremod.agent;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Exact platform-image task boundaries. Unknown JDK/agent bytecode remains unsupported, never guessed. */
public final class JdkTaskBoundaryContract {
    private static final String HOOK="dev/ronova/coremod/agent/BootstrapTaskHooks";
    private record Boundary(String method,String descriptor,boolean execution,int taskLocal) {}
    private static final Map<String,List<Boundary>> TARGETS=Map.of(
        "java/util/concurrent/FutureTask",List.of(new Boundary("run","()V",true,0),new Boundary("runAndReset","()Z",true,0)),
        "java/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask",List.of(new Boundary("run","()V",true,0)),
        "java/util/concurrent/ForkJoinTask",List.of(new Boundary("doExec","()I",true,0),new Boundary("fork","()Ljava/util/concurrent/ForkJoinTask;",false,0)),
        "java/util/concurrent/ThreadPoolExecutor",List.of(new Boundary("execute","(Ljava/lang/Runnable;)V",false,1),
            new Boundary("runWorker","(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V",true,-1)),
        "java/util/concurrent/ScheduledThreadPoolExecutor",List.of(
            new Boundary("delayedExecute","(Ljava/util/concurrent/RunnableScheduledFuture;)V",false,1),
            new Boundary("reExecutePeriodic","(Ljava/util/concurrent/RunnableScheduledFuture;)V",false,1)),
        "java/util/concurrent/ForkJoinPool",List.of(new Boundary("externalSubmit","(Ljava/util/concurrent/ForkJoinTask;)Ljava/util/concurrent/ForkJoinTask;",false,1)),
        "java/util/concurrent/ThreadPoolExecutor$CallerRunsPolicy",List.of(
            new Boundary("rejectedExecution","(Ljava/lang/Runnable;Ljava/util/concurrent/ThreadPoolExecutor;)V",true,-1)));
    private record Images(byte[] original,byte[] instrumented,byte[] originalCanonical,byte[] instrumentedCanonical) {}
    private static final Map<String,Images> IMAGES=new ConcurrentHashMap<>();
    private static final Set<String> DIAGNOSED=ConcurrentHashMap.newKeySet();
    private static volatile boolean enabled;
    private JdkTaskBoundaryContract() {}
    public static boolean registered(String name){return TARGETS.containsKey(name);}
    public static void enable(){enabled=true;}
    public static boolean enabled(){return enabled;}
    public static String platformImageHash(String name) {
        if(!registered(name))throw new IllegalArgumentException("unregistered platform image");
        try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(images(name).originalCanonical));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static boolean verify(String name,byte[] bytes) {
        if(!registered(name)||bytes==null)return false;
        try {return Arrays.equals(images(name).instrumentedCanonical,canonical(bytes));}
        catch(RuntimeException|LinkageError unavailable){return false;}
    }
    public static byte[] transform(String name,byte[] bytes) {
        if(!registered(name)||bytes==null)return null;
        try {
            Images images=images(name);byte[] observed=canonical(bytes);
            if(Arrays.equals(images.instrumentedCanonical,observed))return null;
            if(Arrays.equals(images.originalCanonical,observed))return images.instrumented.clone();
            if(Boolean.getBoolean("ronova.validation.jdkTasks")&&DIAGNOSED.add(name)) {
                try {
                    var directory=java.nio.file.Path.of("ronova","diagnostics");java.nio.file.Files.createDirectories(directory);
                    String prefix=name.replace('/','_');
                    java.nio.file.Files.write(directory.resolve(prefix+"-observed.class"),bytes);
                    java.nio.file.Files.write(directory.resolve(prefix+"-platform.class"),images.original);
                    java.nio.file.Files.write(directory.resolve(prefix+"-expected.class"),images.instrumented);
                } catch(java.io.IOException unavailable){System.err.println("RONOVA_TASK_IMAGE_DIAGNOSTIC_UNAVAILABLE "+unavailable);}
            }
            return null;
        } catch(RuntimeException|LinkageError unavailable){return null;}
    }
    private static Images images(String name){return IMAGES.computeIfAbsent(name,JdkTaskBoundaryContract::load);}
    private static Images load(String name) {
        try(InputStream in=ClassLoader.getPlatformClassLoader().getResourceAsStream(name+".class")) {
            if(in==null)throw new IllegalStateException("platform task image missing");
            byte[] original=in.readNBytes(1024*1024+1);if(original.length>1024*1024)throw new IllegalStateException("platform task image too large");
            var node=new ClassNode();new ClassReader(original).accept(node,ClassReader.EXPAND_FRAMES);
            if(!name.equals(node.name))throw new IllegalStateException("platform task image mismatch");
            for(Boundary boundary:TARGETS.get(name)) {
                var found=node.methods.stream().filter(m->m.name.equals(boundary.method)&&m.desc.equals(boundary.descriptor)).toList();
                if(found.size()!=1)throw new IllegalStateException("platform task ABI unavailable: "+boundary.method);
                MethodNode method=found.get(0);
                if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE|Opcodes.ACC_STATIC))!=0)
                    throw new IllegalStateException("platform task body unavailable");
                if(boundary.taskLocal==-1)instrumentRunnableCall(method);
                else if(boundary.execution)instrumentExecution(method);else instrumentSubmission(method,boundary.taskLocal);
            }
            if(name.equals("java/util/concurrent/FutureTask"))instrumentCancellation(node);
            var writer=new PlatformWriter();node.accept(writer);byte[] patched=writer.toByteArray();
            return new Images(original,patched,canonical(original),canonical(patched));
        } catch(java.io.IOException failure){throw new IllegalStateException(failure);}
    }
    private static void instrumentSubmission(MethodNode method,int local) {
        var entry=new InsnList();entry.add(new VarInsnNode(Opcodes.ALOAD,0));entry.add(new VarInsnNode(Opcodes.ALOAD,local));
        entry.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"submitted","(Ljava/lang/Object;Ljava/lang/Object;)V",false));
        method.instructions.insert(entry);
    }
    private static void instrumentCancellation(ClassNode node) {
        var cancel=node.methods.stream().filter(method->method.name.equals("cancel")&&method.desc.equals("(Z)Z")).findFirst().orElseThrow();
        var calls=Arrays.stream(cancel.instructions.toArray()).filter(instruction->instruction instanceof MethodInsnNode call
            &&call.owner.equals("java/lang/invoke/VarHandle")&&call.name.equals("compareAndSet")
            &&call.desc.equals("(Ljava/util/concurrent/FutureTask;II)Z")).toList();
        if(calls.size()!=1)throw new IllegalStateException("platform Future cancellation CAS unavailable");
        cancel.instructions.set(calls.get(0),new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"cancelState",
            "(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;II)Z",false));
    }
    private static void instrumentRunnableCall(MethodNode method) {
        var calls=Arrays.stream(method.instructions.toArray()).filter(instruction->instruction instanceof MethodInsnNode call
            &&call.getOpcode()==Opcodes.INVOKEINTERFACE&&call.owner.equals("java/lang/Runnable")&&call.name.equals("run")&&call.desc.equals("()V")).toList();
        if(calls.size()!=1)throw new IllegalStateException("platform Runnable dispatch shape unavailable");
        AbstractInsnNode call=calls.get(0);int token=method.maxLocals++,failure=method.maxLocals++;
        var start=new LabelNode();var end=new LabelNode();var handler=new LabelNode();var complete=new LabelNode();
        var enter=new InsnList();enter.add(new InsnNode(Opcodes.DUP));
        enter.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"enter","(Ljava/lang/Object;)Ljava/lang/Object;",false));
        enter.add(new VarInsnNode(Opcodes.ASTORE,token));enter.add(start);method.instructions.insertBefore(call,enter);
        var exit=new InsnList();exit.add(end);appendExit(exit,token);exit.add(new JumpInsnNode(Opcodes.GOTO,complete));
        exit.add(handler);exit.add(new VarInsnNode(Opcodes.ASTORE,failure));appendExit(exit,token);
        exit.add(new VarInsnNode(Opcodes.ALOAD,failure));exit.add(new InsnNode(Opcodes.ATHROW));exit.add(complete);
        method.instructions.insert(call,exit);
        // Close our execution before the JDK's original afterExecute/finally exception handlers run.
        method.tryCatchBlocks.add(0,new TryCatchBlockNode(start,end,handler,"java/lang/Throwable"));
    }
    private static void instrumentExecution(MethodNode method) {
        int token=method.maxLocals++,failure=method.maxLocals++;
        Type result=Type.getReturnType(method.desc);int returned=method.maxLocals;method.maxLocals+=result.getSize();
        var start=new LabelNode();var end=new LabelNode();var handler=new LabelNode();var normal=new LabelNode();
        var entry=new InsnList();entry.add(new VarInsnNode(Opcodes.ALOAD,0));
        entry.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"enter","(Ljava/lang/Object;)Ljava/lang/Object;",false));
        entry.add(new VarInsnNode(Opcodes.ASTORE,token));entry.add(start);
        for(AbstractInsnNode instruction:method.instructions.toArray()) {
            int op=instruction.getOpcode();if(op<Opcodes.IRETURN||op>Opcodes.RETURN)continue;
            var replacement=new InsnList();
            if(result.getSort()!=Type.VOID)replacement.add(new VarInsnNode(result.getOpcode(Opcodes.ISTORE),returned));
            replacement.add(new JumpInsnNode(Opcodes.GOTO,normal));
            method.instructions.insertBefore(instruction,replacement);method.instructions.remove(instruction);
        }
        method.instructions.insert(entry);method.instructions.add(end);method.instructions.add(handler);
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE,failure));appendExit(method.instructions,token);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD,failure));method.instructions.add(new InsnNode(Opcodes.ATHROW));
        method.instructions.add(normal);appendExit(method.instructions,token);
        if(result.getSort()!=Type.VOID)method.instructions.add(new VarInsnNode(result.getOpcode(Opcodes.ILOAD),returned));
        method.instructions.add(new InsnNode(result.getSort()==Type.VOID?Opcodes.RETURN:result.getOpcode(Opcodes.IRETURN)));
        method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/Throwable"));
    }
    private static void appendExit(InsnList code,int token) {
        code.add(new VarInsnNode(Opcodes.ALOAD,token));code.add(new MethodInsnNode(Opcodes.INVOKESTATIC,HOOK,"exit","(Ljava/lang/Object;)V",false));
    }
    private static byte[] canonical(byte[] bytes) {
        var node=new ClassNode();new ClassReader(bytes).accept(node,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        // HotSpot reconstruction omits the legacy Deprecated attribute (ASM pseudo access flag).
        // Runtime annotations and every executable instruction remain part of the comparison.
        node.access&=~Opcodes.ACC_DEPRECATED;
        node.methods.forEach(method->method.access&=~Opcodes.ACC_DEPRECATED);
        node.fields.forEach(field->field.access&=~Opcodes.ACC_DEPRECATED);
        node.methods.sort(Comparator.comparing((MethodNode method)->method.name).thenComparing(method->method.desc));
        node.fields.sort(Comparator.comparing((FieldNode field)->field.name).thenComparing(field->field.desc));
        node.innerClasses.sort(Comparator.comparing(inner->inner.name));
        var writer=new ClassWriter(0);node.accept(writer);return writer.toByteArray();
    }
    private static final class PlatformWriter extends ClassWriter {
        PlatformWriter(){super(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);}
        @Override protected String getCommonSuperClass(String left,String right) {
            try {
                Class<?> a=Class.forName(left.replace('/','.'),false,null),b=Class.forName(right.replace('/','.'),false,null);
                if(a.isAssignableFrom(b))return left;if(b.isAssignableFrom(a))return right;
                if(a.isInterface()||b.isInterface())return "java/lang/Object";
                do{a=a.getSuperclass();}while(!a.isAssignableFrom(b));return a.getName().replace('.','/');
            } catch(ClassNotFoundException failure){throw new IllegalStateException("non-platform task frame type",failure);}
        }
    }
}
