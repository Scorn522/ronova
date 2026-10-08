package dev.ronova.coremod.agent;

/** Called from the native JVMTI ClassFileLoadHook. */
public final class NativeTransformBridge {
    private static final RonovaTransformer TRANSFORMER = new RonovaTransformer("native-jvmti");
    private static final java.util.Map<String,String> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();

    private NativeTransformBridge() {}
    public static boolean verify(String internalName,byte[] classBytes,ClassLoader definingLoader) {
        if(JdkTaskBoundaryContract.registered(internalName)) {
            boolean verified=definingLoader==null&&JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.verify(internalName,classBytes);
            if(verified)FAILURES.remove(internalName);else FAILURES.put(internalName,"jdk-task-platform-image-or-boundary-unverified");
            return verified;
        }
        if(!RonovaTransformer.isTarget(internalName))return false;
        boolean loader=definingLoader==NativeTransformBridge.class.getClassLoader();
        boolean verified=loader&&SemanticGateVerifier.verify(internalName,classBytes);
        if(verified)FAILURES.remove(internalName);
        else FAILURES.put(internalName,loader?SemanticGateVerifier.explainFailure(internalName,classBytes):"defining-loader-mismatch");
        return verified;
    }
    public static java.util.Map<String,String> verificationFailures(){return java.util.Map.copyOf(FAILURES);}

    public static byte[] transform(String internalName, byte[] classBytes) {
        if (!RonovaTransformer.shouldInspect(internalName, classBytes)) {
            return null;
        }
        return TRANSFORMER.transform(
            null,
            NativeTransformBridge.class.getClassLoader(),
            internalName,
            null,
            null,
            classBytes
        );
    }
}
