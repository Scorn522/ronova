package dev.ronova.coremod.agent;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Build-defined field bindings, never a baseline learned from the first live method body. */
final class RawAccessorContract {
    private record Binding(String method, String field, String mappedField, String descriptor, boolean setter, boolean staticField) {}
    private static Binding get(String method,String field,String mapped,String descriptor) { return new Binding(method,field,mapped,descriptor,false,false); }
    private static Binding set(String method,String field,String mapped,String descriptor) { return new Binding(method,field,mapped,descriptor,true,false); }
    private static Binding staticGet(String method,String field,String mapped,String descriptor) {
        return new Binding(method,field,mapped,descriptor,false,true);
    }
    private static final Map<String,List<Binding>> CONTRACTS=Map.ofEntries(
        Map.entry("net/minecraft/world/entity/Entity",List.of(
            get("ronova$getEntityId","id","f_19848_","I"),
            get("ronova$getUuid","uuid","f_19820_","Ljava/util/UUID;"),
            set("ronova$setUuid","uuid","f_19820_","Ljava/util/UUID;"),
            get("ronova$getStringUuid","stringUUID","f_19821_","Ljava/lang/String;"),
            set("ronova$setStringUuid","stringUUID","f_19821_","Ljava/lang/String;"),
            get("ronova$getLevel","level","f_19853_","Lnet/minecraft/world/level/Level;"),
            get("ronova$getRemovalReason","removalReason","f_146795_","Lnet/minecraft/world/entity/Entity$RemovalReason;"),
            set("ronova$setRemovalReason","removalReason","f_146795_","Lnet/minecraft/world/entity/Entity$RemovalReason;"),
            get("ronova$isAddedToWorld","isAddedToWorld","isAddedToWorld","Z"),set("ronova$setAddedToWorld","isAddedToWorld","isAddedToWorld","Z"),
            get("ronova$getEntityData","entityData","f_19804_","Lnet/minecraft/network/syncher/SynchedEntityData;"),
            get("ronova$getLevelCallback","levelCallback","f_146801_","Lnet/minecraft/world/level/entity/EntityInLevelCallback;"),
            set("ronova$setLevelCallback","levelCallback","f_146801_","Lnet/minecraft/world/level/entity/EntityInLevelCallback;"))),
        Map.entry("net/minecraft/world/entity/LivingEntity",List.of(
            staticGet("ronova$getHealthDataAccessor","DATA_HEALTH_ID","f_20961_","Lnet/minecraft/network/syncher/EntityDataAccessor;"),
            get("ronova$isDead","dead","f_20890_","Z"),set("ronova$setDead","dead","f_20890_","Z"))),
        Map.entry("net/minecraft/network/syncher/SynchedEntityData",List.of(
            get("ronova$getOwner","entity","f_135344_","Lnet/minecraft/world/entity/Entity;"),
            get("ronova$getItems","itemsById","f_135345_","Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"),
            get("ronova$getLock","lock","f_135346_","Ljava/util/concurrent/locks/ReadWriteLock;"),set("ronova$setDirty","isDirty","f_135348_","Z"))),
        Map.entry("net/minecraft/network/syncher/SynchedEntityData$DataItem",List.of(
            get("ronova$getAccessor","accessor","f_135390_","Lnet/minecraft/network/syncher/EntityDataAccessor;"),
            get("ronova$getValue","value","f_135391_","Ljava/lang/Object;"),set("ronova$setValue","value","f_135391_","Ljava/lang/Object;"),
            set("ronova$setDirty","dirty","f_135392_","Z"))),
        Map.entry("net/minecraft/world/level/entity/EntityLookup",List.of(
            get("ronova$getById","byId","f_156807_","Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"),get("ronova$getByUuid","byUuid","f_156808_","Ljava/util/Map;"))),
        Map.entry("net/minecraft/world/level/entity/EntityTickList",List.of(
            get("ronova$getActive","active","f_156903_","Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"),
            get("ronova$getPassive","passive","f_156904_","Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"),
            get("ronova$getIterated","iterated","f_156905_","Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"))),
        Map.entry("net/minecraft/world/level/entity/EntitySectionStorage",List.of(
            get("ronova$getSections","sections","f_156852_","Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;"),
            get("ronova$getSectionIds","sectionIds","f_156853_","Lit/unimi/dsi/fastutil/longs/LongSortedSet;"))),
        Map.entry("net/minecraft/world/level/entity/EntitySection",List.of(
            get("ronova$getStorage","storage","f_156827_","Lnet/minecraft/util/ClassInstanceMultiMap;"))),
        Map.entry("net/minecraft/util/ClassInstanceMultiMap",List.of(
            get("ronova$getAllInstances","allInstances","f_13529_","Ljava/util/List;"),get("ronova$getByClass","byClass","f_13527_","Ljava/util/Map;"))),
        Map.entry("net/minecraft/world/level/entity/PersistentEntitySectionManager",List.of(
            get("ronova$getKnownUuids","knownUuids","f_157491_","Ljava/util/Set;"),
            get("ronova$getVisibleStorage","visibleEntityStorage","f_157494_","Lnet/minecraft/world/level/entity/EntityLookup;"),
            get("ronova$getSectionStorage","sectionStorage","f_157495_","Lnet/minecraft/world/level/entity/EntitySectionStorage;"))),
        Map.entry("net/minecraft/server/level/ServerLevel",List.of(
            get("ronova$getPlayers","players","f_8546_","Ljava/util/List;"),
            get("ronova$getEntityTickList","entityTickList","f_143243_","Lnet/minecraft/world/level/entity/EntityTickList;"),
            get("ronova$getEntityManager","entityManager","f_143244_","Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;")))
    );
    static boolean registered(String owner) { return CONTRACTS.containsKey(owner); }
    static boolean verify(ClassNode node) {
        return failureReason(node) == null;
    }
    static String failureReason(ClassNode node) {
        var bindings=CONTRACTS.get(node.name);if(bindings==null)return null;
        for(var binding:bindings) {
            FieldNode field=field(node,binding);MethodNode method=method(node,binding);
            if(field==null)return "raw-field-missing:"+binding.method();
            if(method==null)return "raw-method-missing:"+binding.method();
            if(!exact(node.name,method,field,binding))return "raw-body-mismatch:"+binding.method();
        }
        return null;
    }
    static boolean repair(ClassNode node) {
        var bindings=CONTRACTS.get(node.name);if(bindings==null)return false;boolean changed=false;
        for(var binding:bindings) {
            FieldNode field=field(node,binding);MethodNode method=method(node,binding);
            if(field==null||method==null||(method.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNCHRONIZED))!=0
                || ((method.access&Opcodes.ACC_STATIC)!=0)!=binding.staticField())continue;
            if(exact(node.name,method,field,binding))continue;
            Type type=Type.getType(field.desc);var code=new InsnList();
            if(!binding.staticField())code.add(new VarInsnNode(Opcodes.ALOAD,0));
            if(binding.setter())code.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD),binding.staticField()?0:1));
            code.add(new FieldInsnNode(fieldOpcode(binding),node.name,field.name,field.desc));
            code.add(new InsnNode(binding.setter()?Opcodes.RETURN:type.getOpcode(Opcodes.IRETURN)));
            method.instructions=code;method.tryCatchBlocks.clear();method.localVariables=null;
            method.visibleLocalVariableAnnotations=null;method.invisibleLocalVariableAnnotations=null;
            method.maxLocals=(binding.staticField()?0:1)+(binding.setter()?type.getSize():0);method.maxStack=1+type.getSize();changed=true;
        }
        return changed;
    }
    private static FieldNode field(ClassNode node,Binding binding) {
        FieldNode found=null;
        for(var field:node.fields)if((field.name.equals(binding.field())||field.name.equals(binding.mappedField()))
            &&field.desc.equals(binding.descriptor())&&((field.access&Opcodes.ACC_STATIC)!=0)==binding.staticField()) {if(found!=null)return null;found=field;}
        return found;
    }
    private static MethodNode method(ClassNode node,Binding binding) {
        String descriptor=binding.setter()?"("+binding.descriptor()+")V":"()"+binding.descriptor();MethodNode found=null;
        for(var method:node.methods)if((method.name.equals(binding.method())||generatedStaticAccessor(node,method,binding))
            &&method.desc.equals(descriptor)) {if(found!=null)return null;found=method;}
        return found;
    }
    /** Mixin renames static accessors per session. Metadata identifies the candidate; exact() still proves its body. */
    private static boolean generatedStaticAccessor(ClassNode node,MethodNode method,Binding binding) {
        if(!binding.staticField()||!node.name.equals("net/minecraft/world/entity/LivingEntity")
            ||!binding.method().equals("ronova$getHealthDataAccessor")
            ||(method.access&(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC))
                !=(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_SYNTHETIC)
            ||!method.name.matches(java.util.regex.Pattern.quote(binding.method())+"_\\$md\\$[0-9a-f]{6}\\$[0-9]+"))return false;
        var field=field(node,binding);if(field==null)return false;
        return "dev.ronova.coremod.mixin.LivingEntityStateAccessor".equals(annotationValue(method,
            "Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;","mixin"))
            &&binding.field().equals(annotationValue(method,"Lorg/spongepowered/asm/mixin/gen/Accessor;","value"))
            &&(field.name+":"+field.desc).equals(annotationValue(method,"Lorg/spongepowered/asm/mixin/gen/Accessor;","target"));
    }
    private static Object annotationValue(MethodNode method,String descriptor,String key) {
        AnnotationNode matched=null;
        for(var list:Arrays.asList(method.visibleAnnotations,method.invisibleAnnotations))if(list!=null)
            for(var annotation:list)if(descriptor.equals(annotation.desc)) {if(matched!=null)return null;matched=annotation;}
        if(matched==null||matched.values==null)return null;
        Object value=null;boolean found=false;
        for(int i=0;i+1<matched.values.size();i+=2)if(key.equals(matched.values.get(i))) {
            if(found)return null;found=true;value=matched.values.get(i+1);
        }
        return value;
    }
    private static int fieldOpcode(Binding binding) {
        return binding.staticField() ? (binding.setter()?Opcodes.PUTSTATIC:Opcodes.GETSTATIC)
            : (binding.setter()?Opcodes.PUTFIELD:Opcodes.GETFIELD);
    }
    private static boolean exact(String owner,MethodNode method,FieldNode field,Binding binding) {
        if((method.access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT|Opcodes.ACC_SYNCHRONIZED))!=0
            ||((method.access&Opcodes.ACC_STATIC)!=0)!=binding.staticField()||!method.tryCatchBlocks.isEmpty())return false;
        List<AbstractInsnNode> code=new ArrayList<>();for(var instruction:method.instructions)if(instruction.getOpcode()>=0)code.add(instruction);
        if(code.size()!=(binding.setter()?4:3)-(binding.staticField()?1:0))return false;
        int slot=0;
        if(!binding.staticField()) {
            if(!(code.get(slot++) instanceof VarInsnNode receiver)||receiver.getOpcode()!=Opcodes.ALOAD||receiver.var!=0)return false;
        }
        Type type=Type.getType(field.desc);
        if(binding.setter()) {
            if(!(code.get(slot++) instanceof VarInsnNode argument)||argument.getOpcode()!=type.getOpcode(Opcodes.ILOAD)||argument.var!=(binding.staticField()?0:1))return false;
        }
        if(!(code.get(slot++) instanceof FieldInsnNode access)||access.getOpcode()!=fieldOpcode(binding)
            ||!access.owner.equals(owner)||!access.name.equals(field.name)||!access.desc.equals(field.desc))return false;
        return code.get(slot).getOpcode()==(binding.setter()?Opcodes.RETURN:type.getOpcode(Opcodes.IRETURN));
    }
}
