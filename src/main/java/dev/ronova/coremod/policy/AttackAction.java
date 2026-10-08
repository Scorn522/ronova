package dev.ronova.coremod.policy;

import java.util.Set;

/** Ordinary attack controls; these do not add advanced StrengthOptionKeys. */
public enum AttackAction {
    STRONG_ATTACK("strongAttack", "强力攻击"),
    REWRITE_LIFE("rewriteLife", "改写生命", StrengthOptionKey.ATTACK_STATE_SOURCE),
    TERMINALIZE("terminalize", "强制终结", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER,
        StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
    DIRECT_CLEAR("directClear", "直接清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
    CLEAR_ENTITY_RECORDS("clearEntityRecords", "实体记录清除",
        StrengthOptionKey.ATTACK_STATE_SOURCE, StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
    SUPPRESS_RESPAWN("suppressRespawn", "重生压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
    SUPPRESS_REBUILD("suppressRebuild", "重建压制", StrengthOptionKey.ATTACK_RECOVERY_SOURCE,
        StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
    SUPPRESS_ADMISSION("suppressAdmission", "生成压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
        StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION, StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION),
    ATTACK_RENDER("attackRender", "渲染攻击", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
    CLEAR_TRACKING("clearTracking", "追踪清除", StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION,
        StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
    CLEAR_HUD("clearHud", "血条/HUD 清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
    STRIP_CODE_DEFENSE("stripCodeDefense", "代码防御剥离",
        StrengthOptionKey.ATTACK_RUNTIME_PATCH_DISABLEMENT, StrengthOptionKey.ATTACK_DYNAMIC_CODE,
        StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
    ATTACK_CALL_CHAIN("attackCallChain", "调用链攻击", StrengthOptionKey.ATTACK_LOGIC_SOURCE,
        StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_TASK_CHAIN,
        StrengthOptionKey.ATTACK_CALLBACK_CHAIN, StrengthOptionKey.ATTACK_DISPATCH),
    ALL_RETURN("allReturn", "全返回", StrengthOptionKey.ATTACK_DISPATCH);

    private final String canonical;
    private final String displayName;
    private final Set<StrengthOptionKey> requiredKeys;

    AttackAction(String canonical, String displayName, StrengthOptionKey... requiredKeys) {
        this.canonical = canonical;
        this.displayName = displayName;
        this.requiredKeys = Set.of(requiredKeys);
    }

    public String canonical() { return canonical; }
    public String displayName() { return displayName; }
    public Set<StrengthOptionKey> requiredKeys() { return requiredKeys; }

    public static AttackAction fromCanonical(String value) {
        for (AttackAction action : values()) {
            if (action.canonical.equals(value)) return action;
        }
        throw new IllegalArgumentException("unknown attack action: " + value);
    }
}
