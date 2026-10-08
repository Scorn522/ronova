package dev.ronova.coremod.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/** Independent fixture field list; production repair must never manufacture missing accessors. */
final class RawAccessorScenarios {
    static void entityAccessors(ClassNode node) {
        access(node,"removalReason","Lnet/minecraft/world/entity/Entity$RemovalReason;","ronova$getRemovalReason","ronova$setRemovalReason",false);
        access(node,"isAddedToWorld","Z","ronova$isAddedToWorld","ronova$setAddedToWorld",false);
        access(node,"entityData","Lnet/minecraft/network/syncher/SynchedEntityData;","ronova$getEntityData",null,false);
        access(node,"id","I","ronova$getEntityId",null,false);
        access(node,"uuid","Ljava/util/UUID;","ronova$getUuid","ronova$setUuid",false);
        access(node,"stringUUID","Ljava/lang/String;","ronova$getStringUuid","ronova$setStringUuid",false);
        access(node,"level","Lnet/minecraft/world/level/Level;","ronova$getLevel",null,false);
        access(node,"levelCallback","Lnet/minecraft/world/level/entity/EntityInLevelCallback;","ronova$getLevelCallback","ronova$setLevelCallback",false);
    }
    static void run() {
        dataIdGetter();
        var node=base("net/minecraft/world/entity/Entity");entityAccessors(node);
        require(RawAccessorContract.verify(node),"exact Entity accessors rejected");
        var getter=node.methods.get(0);getter.instructions.clear();
        getter.instructions.add(new InsnNode(Opcodes.ACONST_NULL));getter.instructions.add(new InsnNode(Opcodes.ARETURN));
        require(!RawAccessorContract.verify(node),"all-return raw getter became proof");
        require(RawAccessorContract.repair(node)&&RawAccessorContract.verify(node),"raw getter was not repaired");
        require(!RawAccessorContract.repair(node),"exact accessors are not idempotent");
        node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"unrelatedUuid","Ljava/util/UUID;",null,null));
        var identity=node.methods.stream().filter(method->method.name.equals("ronova$getUuid")).findFirst().orElseThrow();
        identity.instructions.clear();identity.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        identity.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,node.name,"unrelatedUuid","Ljava/util/UUID;"));
        identity.instructions.add(new InsnNode(Opcodes.ARETURN));
        require(!RawAccessorContract.verify(node),"UUID accessor reading another UUID field accepted");
        require(RawAccessorContract.repair(node)&&RawAccessorContract.verify(node),"registered identity getter not repaired");
        var setter=node.methods.get(1);
        setter.tryCatchBlocks.add(new TryCatchBlockNode(new LabelNode(),new LabelNode(),new LabelNode(),null));
        require(!RawAccessorContract.verify(node),"raw setter exception handler accepted");
        require(RawAccessorContract.repair(node)&&setter.tryCatchBlocks.isEmpty(),"raw setter handler not removed");
        getter.access|=Opcodes.ACC_SYNCHRONIZED;
        require(!RawAccessorContract.verify(node),"implicit monitor entered raw accessor proof");
        require(!RawAccessorContract.repair(node),"repair silently changed implicit monitor semantics");
        getter.access&=~Opcodes.ACC_SYNCHRONIZED;
        node.methods.remove(getter);
        int count=node.methods.size();RawAccessorContract.repair(node);
        require(!RawAccessorContract.verify(node)&&count==node.methods.size(),"missing accessor was invented during retransformation");

        node=base("net/minecraft/world/entity/LivingEntity");
        access(node,"f_20961_","Lnet/minecraft/network/syncher/EntityDataAccessor;","ronova$getHealthDataAccessor",null,true);
        access(node,"f_20890_","Z","ronova$isDead","ronova$setDead",false);
        require(RawAccessorContract.verify(node),"mapped static health key rejected");
        generatedStaticAccessor(node);
        var health=node.methods.get(0);
        for(var insn:health.instructions)if(insn instanceof FieldInsnNode field)field.name="unrelated_key";
        require(!RawAccessorContract.verify(node),"different static health key accepted");
        require(RawAccessorContract.repair(node)&&RawAccessorContract.verify(node),"mapped health key not repaired");
        node.fields.add(new FieldNode(Opcodes.ACC_STATIC,"DATA_HEALTH_ID","Lnet/minecraft/network/syncher/EntityDataAccessor;",null,null));
        require(!RawAccessorContract.verify(node),"ambiguous mapped and official health fields accepted");
    }
    private static void generatedStaticAccessor(ClassNode node) {
        var getter=node.methods.stream().filter(m->m.name.equals("ronova$getHealthDataAccessor")).findFirst().orElseThrow();
        getter.name="ronova$getHealthDataAccessor_$md$cdffaf$0";getter.access|=Opcodes.ACC_SYNTHETIC;
        require(!RawAccessorContract.verify(node),"generated-looking name without origin metadata accepted");
        var merged=new AnnotationNode("Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;");
        merged.values=new java.util.ArrayList<>(java.util.List.of("mixin","dev.ronova.coremod.mixin.LivingEntityStateAccessor"));
        var accessor=new AnnotationNode("Lorg/spongepowered/asm/mixin/gen/Accessor;");
        accessor.values=new java.util.ArrayList<>(java.util.List.of("value","DATA_HEALTH_ID","target","f_20961_:Lnet/minecraft/network/syncher/EntityDataAccessor;"));
        getter.visibleAnnotations=new java.util.ArrayList<>(java.util.List.of(merged,accessor));
        require(RawAccessorContract.verify(node),"actual Mixin static accessor layout rejected");
        merged.values.set(1,"unrelated.Mixin");
        require(!RawAccessorContract.verify(node),"foreign Mixin origin accepted");
        merged.values.set(1,"dev.ronova.coremod.mixin.LivingEntityStateAccessor");
        accessor.values.set(3,"other:Lnet/minecraft/network/syncher/EntityDataAccessor;");
        require(!RawAccessorContract.verify(node),"wrong generated accessor target accepted");
        accessor.values.set(3,"f_20961_:Lnet/minecraft/network/syncher/EntityDataAccessor;");
        var duplicate=new MethodNode(getter.access,getter.name.replace("$0","$1"),getter.desc,null,null);
        getter.accept(duplicate);node.methods.add(duplicate);
        require(!RawAccessorContract.verify(node),"ambiguous generated static accessors accepted");
        node.methods.remove(duplicate);
        require(RawAccessorContract.verify(node),"valid static accessor did not recover after ambiguity removal");
    }
    private static void dataIdGetter() {
        for (boolean mapped : new boolean[]{false, true}) {
            var node = base(EntityDataAccessorContract.OWNER); node.version = Opcodes.V17; node.access = Opcodes.ACC_PUBLIC; node.superName = "java/lang/Object";
            String fieldName = mapped ? "f_135010_" : "id", methodName = mapped ? "m_135015_" : "getId";
            node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, "I", null, null));
            var getter = new MethodNode(Opcodes.ACC_PUBLIC, methodName, "()I", null, null);
            getter.instructions.add(new InsnNode(Opcodes.ICONST_0)); getter.instructions.add(new InsnNode(Opcodes.IRETURN)); node.methods.add(getter);
            require(!EntityDataAccessorContract.verify(node), "constant id getter became field proof");
            require(EntityDataAccessorContract.repair(node) && EntityDataAccessorContract.verify(node), "registered id getter not repaired");
            require(!EntityDataAccessorContract.repair(node), "id getter repair not idempotent");
            var writer = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS); node.accept(writer);
            require(RonovaTransformer.isTarget(node.name) && SemanticGateVerifier.verify(node.name, writer.toByteArray()), "id getter not wired into semantic targets");
            require(!NativeTransformBridge.verify(node.name, writer.toByteArray(), new ClassLoader(){}), "foreign loader inherited id proof");
            getter.access |= Opcodes.ACC_SYNCHRONIZED;
            require(!EntityDataAccessorContract.verify(node) && !EntityDataAccessorContract.repair(node), "implicit id getter monitor accepted");
            getter.access &= ~Opcodes.ACC_SYNCHRONIZED;
            node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, mapped ? "id" : "f_135010_", "I", null, null));
            require(!EntityDataAccessorContract.verify(node) && !EntityDataAccessorContract.repair(node), "ambiguous id fields accepted");
            node.fields.remove(node.fields.size() - 1);
            node.methods.clear();
            require(!EntityDataAccessorContract.verify(node) && !EntityDataAccessorContract.repair(node), "missing id getter manufactured");
            node.methods.add(getter); node.name = "example/UnregisteredAccessor";
            require(!EntityDataAccessorContract.verify(node) && !EntityDataAccessorContract.repair(node), "unregistered id-like class accepted");
        }
    }
    private static ClassNode base(String name){var n=new ClassNode();n.name=name;return n;}
    private static void access(ClassNode node,String field,String descriptor,String get,String set,boolean isStatic) {
        int flags=Opcodes.ACC_PUBLIC|(isStatic?Opcodes.ACC_STATIC:0);
        node.fields.add(new FieldNode(flags,field,descriptor,null,null));
        var getter=new MethodNode(flags,get,"()"+descriptor,null,null);
        if(!isStatic)getter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        getter.instructions.add(new FieldInsnNode(isStatic?Opcodes.GETSTATIC:Opcodes.GETFIELD,node.name,field,descriptor));
        getter.instructions.add(new InsnNode(Type.getType(descriptor).getOpcode(Opcodes.IRETURN)));node.methods.add(getter);
        if(set!=null){
            var setter=new MethodNode(flags,set,"("+descriptor+")V",null,null);
            setter.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
            setter.instructions.add(new VarInsnNode(Type.getType(descriptor).getOpcode(Opcodes.ILOAD),1));
            setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,node.name,field,descriptor));
            setter.instructions.add(new InsnNode(Opcodes.RETURN));node.methods.add(setter);
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
