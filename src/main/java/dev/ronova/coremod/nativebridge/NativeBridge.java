package dev.ronova.coremod.nativebridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public final class NativeBridge {
    private static final String RESOURCE = "/native/windows-x86_64/ronova.dll";
    private static final String ROOT_RESOURCE =
        "/native/windows-x86_64/ronova_root.dll";
    private static final int CAP_ROOT_STATE = 0x8;
    private static final int CAP_ROOT_DECISION = 0x10;
    private static final int CAP_ROOT_RECONCILE = 0x20;
    private static final int CAP_SEMANTIC_LEASE = 0x40;
    private static final int ROOT_CAPABILITIES =
        CAP_ROOT_STATE | CAP_ROOT_DECISION | CAP_ROOT_RECONCILE;
    private static volatile State state = State.NOT_ATTEMPTED;
    private static volatile int capabilities;
    private static volatile boolean watchdogRequested;
    private static volatile int watchdogInterval=30000;
    private static volatile Class<?> taskHooks;
    private static final AtomicLong CODE_SEAL_CHALLENGE_SEQUENCE = new AtomicLong(0x5a17c0deL);

    private NativeBridge() {}

    public static synchronized State load(Path gameDirectory) {
        if (state != State.NOT_ATTEMPTED) {
            return state;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win") || !(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            return state = State.UNSUPPORTED_PLATFORM;
        }

        try {
            RuntimeArtifactSource artifacts=RuntimeArtifactSource.resolve(gameDirectory);
            System.load(extract(gameDirectory,artifacts, RESOURCE, "ronova").toString());
            int integrityCapabilities = bootstrap();
            try {
                taskHooks=prepareTaskBridge0(extract(gameDirectory,artifacts,"/META-INF/ronova/task-hook-support.jar","ronova-task-hooks",".jar").toString());
                if(taskHooks!=null){dev.ronova.coremod.agent.JdkTaskBoundaryContract.enable();refreshTaskObservers0();System.out.println("RONOVA_TASK_BRIDGE_READY");}
            } catch(Throwable unavailable){taskHooks=null;System.err.println("RONOVA_TASK_BRIDGE_UNAVAILABLE "+unavailable);}
            // Load the canary before the first registered-code reassertion so the initial challenge
            // does not create its own unverified ClassPrepare gap. The return value is discarded.
            dev.ronova.coremod.integrity.CodeSealProbe.evaluate(0L);
            if(artifacts.codeSealNamespaceCompatible()) {
                try (InputStream sealId=artifacts.open("/META-INF/ronova/code-seal-id.txt")) {
                    if(sealId!=null){String id=new String(sealId.readNBytes(65),java.nio.charset.StandardCharsets.US_ASCII);
                        if(id.matches("[0-9a-f]{64}"))enableCodeSeals0(id);}
                }
            } else {
                System.out.println("RONOVA_CODE_SEAL_DEV_NAMESPACE_UNAVAILABLE mojmap-dev-classes/reobf-sealed-artifact");
            }
            int rootCapabilities = 0;
            try {
                System.load(extract(
                    gameDirectory, artifacts, ROOT_RESOURCE, "ronova_root"
                ).toString());
                rootCapabilities = NativeRootBridge.bootstrapLibrary();
            } catch (Throwable ignored) {
                // Integrity remains available while /ronova status reports Java fallback.
            }
            capabilities = integrityCapabilities | rootCapabilities;
            state = (integrityCapabilities & 0x7) == 0x7
                ? State.ACTIVE : State.JVMTI_LIMITED;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                watchdogRequested=false;
                try {
                    NativeRootBridge.shutdownLibrary();
                } catch (Throwable ignored) {
                    // Root DLL is optional on unsupported or degraded installations.
                }
                try {
                    shutdown();
                } catch (Throwable ignored) {
                    // The operating system will reclaim the library at process exit.
                }
            }, "Ronova-Native-Shutdown"));
        } catch (Throwable ignored) {
            state = State.LOAD_FAILED;
        }
        return state;
    }

    public static boolean isActive() {
        return state == State.ACTIVE;
    }

    public static boolean canRetransform() {
        return (capabilities & 0x4) != 0;
    }

    public static boolean isRootCapable() {
        return isActive() && (capabilities & ROOT_CAPABILITIES) == ROOT_CAPABILITIES;
    }

    public static boolean canAttestEnforcement() {
        return isActive() && (capabilities & CAP_SEMANTIC_LEASE) != 0;
    }

    public static synchronized void startIntegrityWatchdog(int intervalMillis) {
        if (isActive()) {
            int clamped = Math.max(15_000, Math.min(60_000, intervalMillis));
            watchdogRequested=true;watchdogInterval=clamped;
            startWatchdog(clamped);
            if (isRootCapable()) {
                NativeRootBridge.startWatchdog(clamped);
            }
        }
    }

    /** O(1) worker liveness check; Native only replaces a confirmed exited worker. */
    public static void ensureIntegrityWatchdog() {
        if(watchdogRequested&&isActive())startWatchdog(watchdogInterval);
    }

    public static State state() {
        return state;
    }

    public static int capabilityMask() {
        return capabilities;
    }

    private static Path extract(Path gameDirectory, RuntimeArtifactSource artifacts, String resourceName, String prefix)
        throws Exception {
        return extract(gameDirectory,artifacts,resourceName,prefix,".dll");
    }
    private static Path extract(Path gameDirectory,RuntimeArtifactSource artifacts,String resourceName,String prefix,String suffix) throws Exception {
        try (InputStream input = artifacts.open(resourceName)) {
            byte[] resource = input.readAllBytes();
            String digest = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(resource)
            );
            Path nativeDirectory = gameDirectory.resolve("ronova").resolve("native");
            Files.createDirectories(nativeDirectory);
            Path library = nativeDirectory.resolve(
                prefix + "-" + digest.substring(0, 16) + suffix
            );
            if (!Files.isRegularFile(library)
                || !Arrays.equals(Files.readAllBytes(library), resource)) {
                Path temporary = nativeDirectory.resolve(library.getFileName() + ".tmp");
                Files.write(temporary, resource);
                Files.move(temporary, library, StandardCopyOption.REPLACE_EXISTING);
            }
            return library.toAbsolutePath();
        }
    }

    private static native int bootstrap();
    /** Streaming callback while Native retains the same read-only file handle. */
    public interface FileReadObserver {
        boolean allow();
        void accept(byte[] bytes,int length);
    }
    private static native long[] readFileImage0(String path,long limit,FileReadObserver observer);
    private static native long[] savedFileIdentity0(java.io.FileDescriptor descriptor);
    private static native long[] prepareStorageTransaction0(dev.ronova.coremod.observation.RecoverySourceTracker.StorageCommitRequest request);
    private static native long[] finishStorageTransaction0(dev.ronova.coremod.observation.RecoverySourceTracker.StorageCommitRequest request,long token,
        dev.ronova.coremod.observation.RecoverySourceTracker.StoragePreparationResult.StorageWritePermit permit);
    public static long[] prepareStorageTransaction(dev.ronova.coremod.observation.RecoverySourceTracker.StorageCommitRequest request) {
        if(request==null||state!=State.ACTIVE&&state!=State.JVMTI_LIMITED)return new long[]{4,0,0};
        try{return prepareStorageTransaction0(request);}catch(LinkageError unavailable){return new long[]{4,0,0};}
    }
    public static long[] finishStorageTransaction(dev.ronova.coremod.observation.RecoverySourceTracker.StorageCommitRequest request,long token,
        dev.ronova.coremod.observation.RecoverySourceTracker.StoragePreparationResult.StorageWritePermit permit) {
        if(request==null||token<=0)return new long[]{0,0,0};
        try{return finishStorageTransaction0(request,token,permit);}catch(LinkageError unavailable){return new long[]{3,0,0};}
    }
    /** Identity of the actual open platform descriptor. No path lookup and no write authority. */
    public static String savedFileIdentity(java.io.FileDescriptor descriptor) {
        if(descriptor==null||state!=State.ACTIVE&&state!=State.JVMTI_LIMITED)return null;
        try {
            long[] id=savedFileIdentity0(descriptor);
            return id==null||id.length!=3?null:"win32-file-id/1:"+Long.toUnsignedString(id[0],16)+":"
                +Long.toUnsignedString(id[1],16)+":"+Long.toUnsignedString(id[2],16);
        } catch(LinkageError unavailable){return null;}
    }
    /** Null means this Native capability is unavailable; returned status is never a Root receipt. */
    public static long[] readFileImage(Path path,long limit,FileReadObserver observer) {
        if(state!=State.ACTIVE&&state!=State.JVMTI_LIMITED)return null;
        if(path==null||observer==null||limit<1||limit>16L*1024*1024)throw new IllegalArgumentException("invalid file read request");
        try{return readFileImage0(path.toAbsolutePath().normalize().toString(),limit,observer);}
        catch(LinkageError unavailable){return null;}
    }
    private static native Class<?> prepareTaskBridge0(String jarPath);
    private static native void refreshTaskObservers0();
    private static native long[] taskObserverState0();
    private static native boolean currentTaskReceiver0(Object task);
    private static native int registeredFutureKind0(Object task);
    private static native int cancelRegisteredFuture0(Object task,dev.ronova.coremod.root.api.RootExecutionFence.Lease lease,dev.ronova.coremod.root.api.RecoveryTaskPlan plan);
    /** Adapter identity only; it is neither source ownership nor an action permit. */
    public static int registeredFutureKind(Object task) {
        if(task==null||taskHooks==null)return 0;
        try{return registeredFutureKind0(task);}catch(LinkageError unavailable){return 0;}
    }
    /** 0 not executed, 1 accepted, 2 rejected, 3 effect unknown. Root ACK and a current scoped lease are caller obligations. */
    public static int cancelRegisteredFuture(Object task,dev.ronova.coremod.root.api.RootExecutionFence.Lease lease,dev.ronova.coremod.root.api.RecoveryTaskPlan plan) {
        if(task==null||taskHooks==null||lease==null||plan==null||!lease.valid(plan))return 0;
        try{return cancelRegisteredFuture0(task,lease,plan);}catch(LinkageError unavailable){return 0;}
    }
    public static boolean currentTaskReceiver(Object task) {
        if(task==null||taskHooks==null)return false;
        try{return currentTaskReceiver0(task);}catch(LinkageError unavailable){return false;}
    }
    public static Class<?> taskHookClass(){return taskHooks;}
    public record TaskObserverState(boolean supported,long verifiedMask,long generation,long changes,boolean cycling) {
        public boolean covers(long required){return supported&&!cycling&&required>0&&(verifiedMask&required)==required;}
    }
    public static TaskObserverState taskObservers() {
        try {long[] state=taskObserverState0();if(state!=null&&state.length==5)return new TaskObserverState(state[0]==1,state[1],state[2],state[3],state[4]==1);}
        catch(LinkageError unavailable){}
        return new TaskObserverState(false,0,0,0,false);
    }
    private static native boolean enableCodeSeals0(String buildId);
    private static native long[] codeSealState0();
    private static native void refreshCodeSeals0();
    public static void refreshCodeSeals(){if(isActive())refreshCodeSeals0();}
    public record CodeSealState(boolean supported,long registered,long loaded,long verified,long replacements) {
        public boolean current(){return supported&&loaded>0&&loaded==verified;}
    }
    public static CodeSealState codeSeals() {
        try {long[] state=codeSealState0();if(state!=null&&state.length==5)return new CodeSealState(state[0]==1,state[1],state[2],state[3],state[4]);}
        catch(LinkageError unavailable) { }
        return new CodeSealState(false,0,0,0,0);
    }
    /** One bounded behavior challenge of the registered CodeSealProbe. Evidence only; never a lease. */
    public record CodeSealChallengeState(boolean supported,boolean current,long generation,long attempts,long successes,long failures) {
        public CodeSealChallengeState {
            if(generation<0||attempts<0||successes<0||failures<0||successes>attempts||failures>attempts)
                throw new IllegalArgumentException("invalid code seal challenge state");
            current=supported&&current&&successes>0;
        }
    }
    private static native long[] codeSealChallenge0(Class<?> probe,long challenge);
    public static CodeSealChallengeState codeSealChallenge() {
        if(state!=State.ACTIVE&&state!=State.JVMTI_LIMITED)return new CodeSealChallengeState(false,false,0,0,0,0);
        long sequence=CODE_SEAL_CHALLENGE_SEQUENCE.incrementAndGet();
        long challenge=Long.rotateLeft(sequence^System.nanoTime(),23)^0x6a09e667f3bcc909L;
        try {
            long[] values=codeSealChallenge0(dev.ronova.coremod.integrity.CodeSealProbe.class,challenge);
            if(values!=null&&values.length==6)return new CodeSealChallengeState(values[0]==1,values[1]==1,values[2],values[3],values[4],values[5]);
        } catch(LinkageError unavailable) { }
        return new CodeSealChallengeState(false,false,0,0,0,0);
    }
    private static native void startWatchdog(int intervalMillis);
    private static native void shutdown();

    public enum State {
        NOT_ATTEMPTED,
        ACTIVE,
        JVMTI_LIMITED,
        UNSUPPORTED_PLATFORM,
        RESOURCE_MISSING,
        LOAD_FAILED
    }

}
