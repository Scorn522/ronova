package dev.ronova.coremod.agent;

import java.lang.reflect.Method;

/** Runs in a short-lived helper JVM so no jdk.attach.allowAttachSelf flag is required. */
public final class AttachHelper {
    private AttachHelper() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected: <pid> <agent-jar>");
        }
        Class<?> virtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine");
        Method attach = virtualMachine.getMethod("attach", String.class);
        Object connection = attach.invoke(null, arguments[0]);
        try {
            virtualMachine.getMethod("loadAgent", String.class, String.class)
                .invoke(connection, arguments[1], "auto");
        } finally {
            virtualMachine.getMethod("detach").invoke(connection);
        }
    }
}
