package dev.ronova.coremod.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class SemanticGateVerifierSelfTest {
    public static void main(String[] args) {
        String entity="net/minecraft/world/entity/Entity";
        var node=base(entity);
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"fake","Ljava/lang/String;",null,
            "dev/ronova/coremod/agent/BootstrapHooks shouldBlockRemoval shouldBlockRevive"));
        method(node,"remove","(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",Opcodes.RETURN);
        method(node,"revive","()V",Opcodes.RETURN);
        RawAccessorScenarios.entityAccessors(node);
        byte[] raw=bytes(node);
        require(!SemanticGateVerifier.verify(entity,raw),"constant-pool strings became semantic proof");
        var transformer=new RonovaTransformer("semantic-test");
        byte[] guarded=transformer.transform(null,null,entity,null,null,raw);
        require(SemanticGateVerifier.verify(entity,guarded),"registered entry guards not recognized");
        require(transformer.transform(null,null,entity,null,null,guarded)==null,"valid gates were repeatedly injected");
        var poisoned=read(guarded);
        for(var insn:poisoned.methods.get(0).instructions)if(insn instanceof JumpInsnNode jump){jump.setOpcode(Opcodes.IFNE);break;}
        require(!SemanticGateVerifier.verify(entity,bytes(poisoned)),"reversed permission branch passed");
        byte[] repaired=transformer.transform(null,null,entity,null,null,bytes(poisoned));
        require(SemanticGateVerifier.verify(entity,repaired),"invalid guard presence prevented repair");
        require(!NativeTransformBridge.verify(entity,guarded,new ClassLoader(){}),"foreign defining loader inherited a proof");
        String level="net/minecraft/world/level/Level";node=base(level);
        method(node,"query","()Ljava/util/List;",Opcodes.ARETURN);
        raw=bytes(node);guarded=transformer.transform(null,null,level,null,null,raw);
        require(SemanticGateVerifier.verify(level,guarded),"return filter not recognized");
        poisoned=read(guarded);var query=poisoned.methods.get(0);var ret=query.instructions.getLast();
        var bypass=new LabelNode();query.instructions.insertBefore(ret,bypass);
        var branch=new InsnList();branch.add(new InsnNode(Opcodes.ACONST_NULL));branch.add(new JumpInsnNode(Opcodes.GOTO,bypass));query.instructions.insert(branch);
        require(!SemanticGateVerifier.verify(level,bytes(poisoned)),"branch directly into return bypassed the filter proof");
        repaired=transformer.transform(null,null,level,null,null,bytes(poisoned));
        require(SemanticGateVerifier.verify(level,repaired),"new raw return path was not repaired");
        require(!SemanticGateVerifier.verify(entity,repaired),"class identity mismatch accepted");
        require(!SemanticGateVerifier.verify(level,new byte[]{0,1,2}),"malformed bytes accepted");
        RawAccessorScenarios.run();
        NbtContractScenarios.run();
        System.out.println("SemanticGateVerifierSelfTest: PASS (fake strings, branch polarity, all return edges, repair/idempotence, defining loader, malformed image)");
    }
    private static ClassNode base(String name){var node=new ClassNode();node.version=Opcodes.V17;node.access=Opcodes.ACC_PUBLIC;node.name=name;node.superName="java/lang/Object";return node;}
    private static void method(ClassNode node,String name,String desc,int ret){var m=new MethodNode(Opcodes.ACC_PUBLIC,name,desc,null,null);if(ret==Opcodes.ARETURN)m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));m.instructions.add(new InsnNode(ret));node.methods.add(m);}
    private static byte[] bytes(ClassNode node){var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();}
    private static ClassNode read(byte[] bytes){var node=new ClassNode();new ClassReader(bytes).accept(node,0);return node;}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
