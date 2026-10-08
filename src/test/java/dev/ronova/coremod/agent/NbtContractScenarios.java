package dev.ronova.coremod.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

final class NbtContractScenarios {
    static void run() {
        var copy=copyFixture();
        require(!NbtCopyContract.verify(copy),"uninstrumented copy became observation evidence");
        require(NbtCopyContract.patch(copy)&&NbtCopyContract.verify(copy),"registered copy algorithm rejected");
        require(!NbtCopyContract.patch(copy),"copy hook reinjected");
        var method=copy.methods.get(1);
        var lambda=(InvokeDynamicInsnNode)method.instructions.get(2);
        var genuine=lambda.bsmArgs[1];
        lambda.bsmArgs[1]=new Handle(Opcodes.H_INVOKEINTERFACE,"net/minecraft/nbt/Tag","unrelated","()Lnet/minecraft/nbt/Tag;",true);
        require(!NbtCopyContract.verify(copy),"different lambda body inherited copy proof");
        lambda.bsmArgs[1]=genuine;
        require(NbtCopyContract.verify(copy),"restored copy not recognized");
        for(var instruction:copy.methods.get(0).instructions)if(instruction instanceof FieldInsnNode field)field.name="otherMap";
        require(!NbtCopyContract.verify(copy),"constructor writing unrelated map accepted");
        copy=copyFixture();method=copy.methods.get(1);method.instructions.clear();
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));method.instructions.add(new InsnNode(Opcodes.ARETURN));
        NbtCopyContract.patch(copy);
        require(!NbtCopyContract.verify(copy),"hook-only return-this became independent copy evidence");

        var factory=new ClassNode();factory.name=NbtFactoryContract.OWNER;
        method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"create",
            "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",null,null);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/util/Optional","empty","()Ljava/util/Optional;",false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));factory.methods.add(method);
        require(!NbtFactoryContract.verify(factory),"unguarded factory accepted");
        require(NbtFactoryContract.patch(factory)&&NbtFactoryContract.verify(factory),"factory guard rejected");
        require(!NbtFactoryContract.patch(factory),"factory guard reinjected");
        var jump=(JumpInsnNode)method.instructions.get(3);jump.setOpcode(Opcodes.IFNE);
        require(!NbtFactoryContract.verify(factory),"reversed NBT denial accepted");jump.setOpcode(Opcodes.IFEQ);
        var bypass=new LabelNode();method.instructions.insertBefore(method.instructions.get(5),bypass);jump.label=bypass;
        require(!NbtFactoryContract.verify(factory),"factory false path skips original body");
    }
    private static ClassNode copyFixture() {
        var node=new ClassNode();node.name=NbtCopyContract.OWNER;
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE,"tags","Ljava/util/Map;",null,null));
        var init=new MethodNode(Opcodes.ACC_PUBLIC,"<init>","(Ljava/util/Map;)V",null,null);
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));init.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
        init.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,node.name,"tags","Ljava/util/Map;"));
        init.instructions.add(new InsnNode(Opcodes.RETURN));node.methods.add(init);
        var copy=new MethodNode(Opcodes.ACC_PUBLIC,"copy","()Lnet/minecraft/nbt/CompoundTag;",null,null);
        copy.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        copy.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,node.name,"tags","Ljava/util/Map;"));
        var bootstrap=new Handle(Opcodes.H_INVOKESTATIC,"java/lang/invoke/LambdaMetafactory","metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",false);
        copy.instructions.add(new InvokeDynamicInsnNode("apply","()Lcom/google/common/base/Function;",bootstrap,
            Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"),
            new Handle(Opcodes.H_INVOKEINTERFACE,"net/minecraft/nbt/Tag","copy","()Lnet/minecraft/nbt/Tag;",true),
            Type.getMethodType("(Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;")));
        copy.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/google/common/collect/Maps","transformValues",
            "(Ljava/util/Map;Lcom/google/common/base/Function;)Ljava/util/Map;",false));
        copy.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/google/common/collect/Maps","newHashMap","(Ljava/util/Map;)Ljava/util/HashMap;",false));
        copy.instructions.add(new VarInsnNode(Opcodes.ASTORE,1));
        copy.instructions.add(new TypeInsnNode(Opcodes.NEW,node.name));copy.instructions.add(new InsnNode(Opcodes.DUP));
        copy.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
        copy.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,node.name,"<init>","(Ljava/util/Map;)V",false));
        copy.instructions.add(new InsnNode(Opcodes.ARETURN));node.methods.add(copy);return node;
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
