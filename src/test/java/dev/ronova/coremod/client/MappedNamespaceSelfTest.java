package dev.ronova.coremod.client;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards reobfuscation across every class namespace present in the 1.20.1 mappings. */
public final class MappedNamespaceSelfTest {
    private MappedNamespaceSelfTest() {}

    public static void main(String[] args) throws Exception {
        String resource = "dev/ronova/coremod/client/ClientInputController.class";
        try (InputStream input = MappedNamespaceSelfTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            require(input != null, "missing production ClientInputController.class");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);

            int minecraftWindow = 0;
            int nativeHandle = 0;
            int keyChecks = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof MethodInsnNode invocation)) {
                        continue;
                    }
                    if ("net/minecraft/client/Minecraft".equals(invocation.owner)
                        && "()Lcom/mojang/blaze3d/platform/Window;".equals(invocation.desc)) {
                        require("m_91268_".equals(invocation.name),
                            "Minecraft.getWindow was not reobfuscated: " + invocation.name);
                        minecraftWindow++;
                    }
                    if ("com/mojang/blaze3d/platform/Window".equals(invocation.owner)
                        && "()J".equals(invocation.desc)) {
                        require("m_85439_".equals(invocation.name),
                            "Window.getWindow was not reobfuscated: " + invocation.name);
                        nativeHandle++;
                    }
                    if ("com/mojang/blaze3d/platform/InputConstants".equals(invocation.owner)
                        && "(JI)Z".equals(invocation.desc)) {
                        require("m_84830_".equals(invocation.name),
                            "InputConstants.isKeyDown was not reobfuscated: "
                                + invocation.name);
                        keyChecks++;
                    }
                }
            }
            require(minecraftWindow > 0,
                "missing Minecraft window lookups");
            require(nativeHandle == minecraftWindow,
                "incomplete native window-handle chains: " + nativeHandle + "/" + minecraftWindow);
            require(keyChecks == 2,
                "expected two mapped key checks, found " + keyChecks);
        }
        System.out.println(
            "MappedNamespaceSelfTest: PASS (all window chains mapped, 2 key checks)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
