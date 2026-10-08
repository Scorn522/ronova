package dev.ronova.coremod.policy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Stable 7.16 policy vocabulary. A key existing does not claim its executor is active. */
public enum StrengthOptionKey {
    ATTACK_NON_ENTITY("attack.non_entity", PlaneId.ATTACK, false),
    ATTACK_LOGIC_SOURCE("attack.logic_source", PlaneId.ATTACK, false),
    ATTACK_RECOVERY_SOURCE("attack.recovery_source", PlaneId.ATTACK, false),
    ATTACK_STATE_SOURCE("attack.state_source", PlaneId.ATTACK, false),
    ATTACK_REGISTRATION_SOURCE("attack.registration_source", PlaneId.ATTACK, false),
    ATTACK_TASK_CHAIN("attack.task_chain", PlaneId.ATTACK, false),
    ATTACK_CALLBACK_CHAIN("attack.callback_chain", PlaneId.ATTACK, false),
    ATTACK_NETWORK_RECONSTRUCTION("attack.network_reconstruction", PlaneId.ATTACK, false),
    ATTACK_SAVE_RECONSTRUCTION("attack.save_reconstruction", PlaneId.ATTACK, false),
    ATTACK_DYNAMIC_CODE("attack.dynamic_code", PlaneId.ATTACK, true),
    ATTACK_DISPATCH("attack.dispatch", PlaneId.ATTACK, true),
    ATTACK_RUNTIME_PATCH_DISABLEMENT("attack.runtime_patch_disablement", PlaneId.ATTACK, true),
    ATTACK_TARGET_CLASS_LINEAGE("attack.target_class_lineage", PlaneId.ATTACK, true),
    ATTACK_FINAL_RESULT_TAKEOVER("attack.final_result_takeover", PlaneId.ATTACK, false),
    ATTACK_SOURCE_TRACE("attack.source_trace", PlaneId.ATTACK, true),

    INTERCEPT_MIXIN("intercept.mixin", PlaneId.INTERCEPTION, true),
    INTERCEPT_ASM("intercept.asm", PlaneId.INTERCEPTION, true),
    INTERCEPT_INSTRUMENTATION("intercept.instrumentation", PlaneId.INTERCEPTION, true),
    INTERCEPT_JVMTI("intercept.jvmti", PlaneId.INTERCEPTION, true),
    INTERCEPT_UNSAFE_REFLECTION("intercept.unsafe_reflection", PlaneId.INTERCEPTION, true),
    INTERCEPT_CLASSLOADER("intercept.classloader", PlaneId.INTERCEPTION, true),
    INTERCEPT_JNI_NATIVE("intercept.jni_native", PlaneId.INTERCEPTION, true),
    INTERCEPT_MODLAUNCHER("intercept.modlauncher", PlaneId.INTERCEPTION, true),
    INTERCEPT_THREAD_TASK("intercept.thread_task", PlaneId.INTERCEPTION, false),
    INTERCEPT_ALL_RETURN("intercept.all_return", PlaneId.INTERCEPTION, true),

    PROTECT_LIFE("protect.life", PlaneId.PROTECTION, false),
    PROTECT_DEATH("protect.death", PlaneId.PROTECTION, false),
    PROTECT_REMOVAL("protect.removal", PlaneId.PROTECTION, false),
    PROTECT_ENTITY_RECORD("protect.entity_record", PlaneId.PROTECTION, false),
    PROTECT_IDENTITY_RESPAWN("protect.identity_respawn", PlaneId.PROTECTION, false),
    PROTECT_DISPLAY("protect.display", PlaneId.PROTECTION, false),
    PROTECT_ANTI_INTERCEPTION("protect.anti_interception", PlaneId.PROTECTION, true),
    PROTECT_ANTI_ALL_RETURN("protect.anti_all_return", PlaneId.PROTECTION, true),
    PROTECT_INPUT_COMMUNICATION("protect.input_communication", PlaneId.PROTECTION, false),
    PROTECT_CALL("protect.call", PlaneId.PROTECTION, true),
    PROTECT_CODE_INTEGRITY("protect.code_integrity", PlaneId.PROTECTION, true),
    PROTECT_CORE_AUTHORITY("protect.core_authority", PlaneId.PROTECTION, true),
    PROTECT_AUTO_REPAIR("protect.auto_repair", PlaneId.PROTECTION, true),
    PROTECT_STATE_RECOVERY("protect.state_recovery", PlaneId.PROTECTION, false),
    PROTECT_RESTART_RECOVERY("protect.restart_recovery", PlaneId.PROTECTION, true),
    PROTECT_ROLLBACK("protect.rollback", PlaneId.PROTECTION, true),
    PROTECT_LOOP_GUARD("protect.loop_guard", PlaneId.PROTECTION, false),

    RUNTIME_JAVA_AGENT("runtime.java_agent", PlaneId.RUNTIME, true),
    RUNTIME_INSTRUMENTATION("runtime.instrumentation", PlaneId.RUNTIME, true),
    RUNTIME_JVMTI("runtime.jvmti", PlaneId.RUNTIME, true),
    RUNTIME_NATIVE("runtime.native", PlaneId.RUNTIME, true),
    RUNTIME_NATIVE_LOADER("runtime.native_loader", PlaneId.RUNTIME, true),
    RUNTIME_RENDER_GUARD("runtime.render_guard", PlaneId.RUNTIME, true),
    RUNTIME_CRITICAL_CLASS_WATCH("runtime.critical_class_watch", PlaneId.RUNTIME, true),
    RUNTIME_DYNAMIC_CODE_WATCH("runtime.dynamic_code_watch", PlaneId.RUNTIME, true),
    RUNTIME_NATIVE_MODULE_WATCH("runtime.native_module_watch", PlaneId.RUNTIME, true),
    RUNTIME_THREAD_CONTINUITY("runtime.thread_continuity", PlaneId.RUNTIME, false),
    RUNTIME_BACKGROUND_INTEGRITY("runtime.background_integrity", PlaneId.RUNTIME, true),
    RUNTIME_ANOMALOUS_CALL_LOG("runtime.anomalous_call_log", PlaneId.RUNTIME, false);

    private static final Map<String, StrengthOptionKey> BY_CANONICAL = index();
    private final String canonical;
    private final PlaneId plane;
    private final boolean restartRequired;

    StrengthOptionKey(String canonical, PlaneId plane, boolean restartRequired) {
        this.canonical = canonical;
        this.plane = plane;
        this.restartRequired = restartRequired;
    }

    public String canonical() { return canonical; }
    public PlaneId plane() { return plane; }
    public boolean restartRequired() { return restartRequired; }

    public static StrengthOptionKey fromCanonical(String value) {
        StrengthOptionKey key = value == null ? null : BY_CANONICAL.get(value.trim());
        if (key == null) {
            throw new IllegalArgumentException("unknown strength option: " + value);
        }
        return key;
    }

    private static Map<String, StrengthOptionKey> index() {
        Map<String, StrengthOptionKey> values = new LinkedHashMap<>();
        for (StrengthOptionKey key : values()) {
            if (values.put(key.canonical, key) != null) {
                throw new ExceptionInInitializerError("duplicate strength option " + key.canonical);
            }
        }
        return Map.copyOf(values);
    }
}
