package dev.ronova.coremod.integrity;

import java.lang.reflect.Modifier;

/** Verifying a base method body does not verify a subclass's virtual override. */
public final class RawAccessorDispatch {
    private RawAccessorDispatch() {}
    public static boolean matches(Object receiver, Class<?> owner, Class<?> accessor) {
        if (receiver == null || owner == null || accessor == null || !accessor.isInterface()
            || !owner.isInstance(receiver) || !accessor.isInstance(receiver)) return false;
        try {
            int checked = 0;
            for (var method : accessor.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) continue;
                if (!method.getName().startsWith("ronova$")) return false;
                checked++;
                var implementation = receiver.getClass().getMethod(method.getName(), method.getParameterTypes());
                if (implementation.getDeclaringClass() != owner || implementation.getReturnType() != method.getReturnType()
                    || Modifier.isAbstract(implementation.getModifiers()) || Modifier.isNative(implementation.getModifiers())) return false;
            }
            return checked > 0;
        } catch (ReflectiveOperationException | SecurityException | LinkageError unavailable) { return false; }
    }
}
