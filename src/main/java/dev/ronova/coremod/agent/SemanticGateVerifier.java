package dev.ronova.coremod.agent;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Verifies registered gate control flow, not constant-pool strings or arbitrary JVM integrity. */
public final class SemanticGateVerifier {
    private static final String HOOKS="dev/ronova/coremod/agent/BootstrapHooks";
    private SemanticGateVerifier() {}
    /** Diagnostic only: the full verifier remains the authority for acceptance. */
    public static String explainFailure(String name, byte[] bytes) {
        if(name==null||bytes==null||bytes.length>8*1024*1024)return "invalid-image";
        try {
            var node=new ClassNode();new ClassReader(bytes).accept(node,0);
            if(!name.equals(node.name))return "class-name-mismatch";
            String raw=RawAccessorContract.failureReason(node);if(raw!=null)return raw;
            if(name.equals("net/minecraft/world/entity/LivingEntity")) {
                if(!required(node,Set.of("hurt","m_6469_"),"(Lnet/minecraft/world/damagesource/DamageSource;F)Z","shouldBlockDamage",0))return "living-damage-gate";
                if(!required(node,Set.of("die","m_6667_"),"(Lnet/minecraft/world/damagesource/DamageSource;)V","shouldBlockDeath",0))return "living-death-gate";
                if(!required(node,Set.of("setHealth","m_21153_"),"(F)V","shouldBlockHealth",Opcodes.FLOAD))return "living-health-gate";
            }
            return "registered-gate-control-flow-unproven";
        }catch(RuntimeException malformed){return "malformed-class-image";}
    }
    public static boolean verify(String name,byte[] bytes) {
        if(name==null||bytes==null||bytes.length>8*1024*1024)return false;
        try {
            var node=new ClassNode();new ClassReader(bytes).accept(node,0);if(!name.equals(node.name))return false;
            if(!RawAccessorContract.verify(node))return false;
            if(name.equals(EntityDataAccessorContract.OWNER))return EntityDataAccessorContract.verify(node);
            if(name.equals(NbtCopyContract.OWNER))return NbtCopyContract.verify(node);
            if(name.equals(NbtFactoryContract.OWNER))return NbtFactoryContract.verify(node);
            if(name.equals("net/minecraft/world/entity/Entity"))return required(node,Set.of("remove","m_142687_"),
                "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V","shouldBlockRemoval",Opcodes.ALOAD)
                &&required(node,Set.of("revive"),"()V","shouldBlockRevive",0);
            if(name.equals("net/minecraft/world/entity/LivingEntity"))return required(node,Set.of("hurt","m_6469_"),
                "(Lnet/minecraft/world/damagesource/DamageSource;F)Z","shouldBlockDamage",0)
                &&required(node,Set.of("die","m_6667_"),"(Lnet/minecraft/world/damagesource/DamageSource;)V","shouldBlockDeath",0)
                &&required(node,Set.of("setHealth","m_21153_"),"(F)V","shouldBlockHealth",Opcodes.FLOAD);
            if(!Set.of("net/minecraft/world/level/Level","net/minecraft/server/level/ServerLevel","net/minecraft/client/multiplayer/ClientLevel").contains(name))
                return RawAccessorContract.registered(name);
            int checked=0;
            for(var method:node.methods) {
                if(method.name.startsWith("ronova$"))continue; // Raw observers intentionally bypass gameplay Presence filtering.
                String type=Type.getReturnType(method.desc).getDescriptor();String cast;
                if(type.equals("Ljava/util/List;")||type.equals("Ljava/lang/Iterable;"))cast=type.substring(1,type.length()-1);
                else if(type.equals("Lnet/minecraft/world/entity/Entity;"))cast="net/minecraft/world/entity/Entity";
                else continue;
                if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))!=0)continue;
                for(var instruction:method.instructions)if(instruction.getOpcode()==Opcodes.ARETURN) {
                    checked++;if(!filteredReturn(method,instruction,cast.equals("net/minecraft/world/entity/Entity")?"filterQuarantinedEntity":"filterQuarantinedResult",cast))return false;
                }
            }
            return checked>0;
        }catch(RuntimeException invalid){return false;}
    }
    private static boolean required(ClassNode node,Set<String> names,String descriptor,String hook,int argumentOpcode) {
        int found=0;
        for(var method:node.methods)if(names.contains(method.name)&&method.desc.equals(descriptor)) {
            found++;if(!guarded(method,hook,argumentOpcode))return false;
        }
        return found>0;
    }
    static boolean guarded(MethodNode method,String hook,int argumentOpcode) {
        if((method.access&(Opcodes.ACC_STATIC|Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))!=0)return false;
        var real=new ArrayList<AbstractInsnNode>();for(var insn:method.instructions)if(insn.getOpcode()>=0)real.add(insn);
        int i=0;
        if(real.size()<5||!variable(real.get(i++),Opcodes.ALOAD,0))return false;
        if(argumentOpcode!=0&&!variable(real.get(i++),argumentOpcode,1))return false;
        String desc=argumentOpcode==Opcodes.ALOAD?"(Ljava/lang/Object;Ljava/lang/Object;)Z":argumentOpcode==Opcodes.FLOAD?"(Ljava/lang/Object;F)Z":"(Ljava/lang/Object;)Z";
        if(!call(real.get(i++),hook,desc))return false;
        if(!(real.get(i++) instanceof JumpInsnNode branch)||branch.getOpcode()!=Opcodes.IFEQ)return false;
        boolean bool=Type.getReturnType(method.desc).getSort()==Type.BOOLEAN;
        if(bool&&real.get(i++).getOpcode()!=Opcodes.ICONST_0)return false;
        if(real.get(i++).getOpcode()!=(bool?Opcodes.IRETURN:Opcodes.RETURN)||i>=real.size())return false;
        if(nextReal(branch.label)!=real.get(i))return false;
        int gateEnd=method.instructions.indexOf(real.get(i));
        for(var handler:method.tryCatchBlocks)if(method.instructions.indexOf(handler.start)<gateEnd)return false;
        return true;
    }
    static boolean filteredReturn(MethodNode method,AbstractInsnNode ret,String hook,String cast) {
        var type=previousReal(ret);var invoke=previousReal(type);
        if(!(type instanceof TypeInsnNode check)||check.getOpcode()!=Opcodes.CHECKCAST||!check.desc.equals(cast)
            ||!call(invoke,hook,"(Ljava/lang/Object;)Ljava/lang/Object;"))return false;
        int from=method.instructions.indexOf(invoke),to=method.instructions.indexOf(ret);
        for(var insn:method.instructions) {
            if(insn instanceof JumpInsnNode branch&&inside(method,branch.label,from,to))return false;
            if(insn instanceof TableSwitchInsnNode table&&(inside(method,table.dflt,from,to)||table.labels.stream().anyMatch(l->inside(method,l,from,to))))return false;
            if(insn instanceof LookupSwitchInsnNode table&&(inside(method,table.dflt,from,to)||table.labels.stream().anyMatch(l->inside(method,l,from,to))))return false;
        }
        for(var handler:method.tryCatchBlocks)if(inside(method,handler.handler,from,to))return false;
        return true;
    }
    private static boolean inside(MethodNode m,LabelNode label,int from,int to){int n=m.instructions.indexOf(label);return n>from&&n<=to;}
    private static boolean variable(AbstractInsnNode i,int opcode,int slot){return i instanceof VarInsnNode v&&v.getOpcode()==opcode&&v.var==slot;}
    private static boolean call(AbstractInsnNode i,String name,String desc){return i instanceof MethodInsnNode m&&m.getOpcode()==Opcodes.INVOKESTATIC&&!m.itf&&m.owner.equals(HOOKS)&&m.name.equals(name)&&m.desc.equals(desc);}
    private static AbstractInsnNode previousReal(AbstractInsnNode i){if(i==null)return null;do{i=i.getPrevious();}while(i!=null&&i.getOpcode()<0);return i;}
    private static AbstractInsnNode nextReal(AbstractInsnNode i){while(i!=null&&i.getOpcode()<0)i=i.getNext();return i;}
}
