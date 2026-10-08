package dev.ronova.coremod.client.tooltip;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Approved compact/Shift copy expressed as deterministic, explicitly laid-out rows. */
public final class RonovaTooltipText {
    private static final int COMPACT_WIDTH = 248;
    private static final int EXPANDED_WIDTH = 282;

    private static final int LORE_ASH = 0xC2B8BD;
    private static final int LORE_ROSE = 0xD0AFBA;
    private static final int LORE_COVENANT = 0xD88798;
    private static final int LORE_FINAL = 0xF0CBD5;
    private static final int LORE_ABYSS = 0xC48AD0;
    private static final int LORE_CONTRACT = 0x78C9D9;

    /** A single coherent Relic-style palette, mixed into each semantic base colour. */
    private static final int[] RONOVA_PALETTE = {
        0x7A1830, 0xE43A5B, 0xD57AD8, 0xE7DEE1
    };

    private RonovaTooltipText() {}

    public static int targetWidth(boolean expanded) {
        return expanded ? EXPANDED_WIDTH : COMPACT_WIDTH;
    }

    public static List<TooltipComponent> build(boolean expanded) {
        return expanded ? expanded() : compact();
    }

    private static List<TooltipComponent> compact() {
        List<TooltipComponent> lines = new ArrayList<>();
        text(lines, gradientTitle("若娜瓦"), 11);
        text(lines, line("[ 原初四影 ·「死之执政」的真理权柄 ]", 0x918991, false));
        spacer(lines, 4);
        divider(lines, COMPACT_WIDTH);
        text(lines, paletteText(
            "「死之执政」若娜瓦遗落的一缕权能。",
            0xD45A70, 0.24F, 7600L, 0.022F, false, false));
        text(lines, line("挥动它，便是向死亡预支力量。", LORE_ROSE, false));
        divider(lines, COMPACT_WIDTH);
        spacer(lines, 4);
        text(lines, paletteText(
            "【至高权柄 · 持久身份裁决】",
            0xD57AD8, 0.38F, 6800L, 0.040F, false, false), 11);
        lines.add(control("左键", "「赤瞳注视」", "64 格单体 · 死期践约", false, false));
        lines.add(control("右键", "「降下死律」", "64 格界域 · 万物皆死", false, false));
        lines.add(control(
            "Shift + 右键",
            "「宣告此界的『死亡』」",
            "裁决当前维度所有实体",
            true,
            true
        ));
        spacer(lines, 5);
        text(lines, paletteText(
            "✦ 原初死契：放入物品栏即绑定持有者连续性",
            0x72D5E5, 0.22F, 8200L, 0.020F, false, false));
        text(lines, line("  （不可剥离）", 0x72D5E5, false));
        spacer(lines, 3);
        text(lines, line("[ 按住 Shift 查看契约真意 ]", 0x938B92, true));
        return lines;
    }

    private static List<TooltipComponent> expanded() {
        List<TooltipComponent> lines = new ArrayList<>();
        text(lines, gradientTitle("若娜瓦 · 终末之忆"), 11);
        text(lines, line("[ 原初名册中关于「若娜瓦」的记载 ]", 0x918991, false));
        spacer(lines, 4);
        divider(lines, EXPANDED_WIDTH);
        spacer(lines, 3);

        text(lines, rich(
            segment("「原初的", LORE_ASH),
            segment("法涅斯", LORE_ROSE),
            segment("降下四个发光的影子，", LORE_ASH)));
        text(lines, rich(
            segment("其中之一名为", LORE_ASH),
            segment("若娜瓦", LORE_ROSE),
            segment("，司掌万物", LORE_ASH),
            segment("终结", LORE_COVENANT),
            segment("之权能。", LORE_ASH)));
        text(lines, rich(
            segment("她不关心", LORE_ASH),
            segment("死亡", LORE_COVENANT),
            segment("的时间与形式，只确保那结局终将到来。", LORE_ASH)));

        spacer(lines, 5);
        text(lines, rich(
            segment("纳塔的初代火神", LORE_ASH),
            segment("希巴拉克", LORE_ROSE),
            segment("曾向她求取力量，", LORE_ASH)));
        text(lines, rich(
            segment("用以构筑对抗", LORE_ASH),
            segment("深渊", LORE_ABYSS),
            segment("的规则。", LORE_ASH)));
        text(lines, rich(
            segment("若娜瓦应允了，却立下不可违背的", LORE_ASH),
            segment("铁律", LORE_COVENANT),
            segment("：", LORE_ASH)));
        text(lines, rich(
            segment("借力者，须以自身的", LORE_ASH),
            segment("死亡", LORE_COVENANT),
            segment("作为最终的结算。", LORE_ASH)));

        spacer(lines, 5);
        text(lines, rich(
            segment("此剑便是那份", LORE_ASH),
            segment("契约", LORE_CONTRACT),
            segment("的碎片——剑格上的", LORE_ASH),
            segment("赤瞳", LORE_COVENANT),
            segment("缓缓睁开，", LORE_ASH)));
        text(lines, rich(
            segment("那是「", LORE_ASH),
            segment("死之执政", LORE_COVENANT),
            segment("」的目光。", LORE_ASH)));
        text(lines, rich(
            segment("持有它，便意味着你已提前签署了", LORE_ASH),
            segment("终末的账单", LORE_COVENANT),
            segment("。", LORE_ASH)));
        text(lines, rich(
            segment("向死而生，向生借死，这便是「", LORE_FINAL),
            segment("若娜瓦", 0xF3A9BA),
            segment("」的全部真意。」", LORE_FINAL)));
        return lines;
    }

    private static RonovaTooltipControlData control(
        String input,
        String action,
        String description,
        boolean terminal,
        boolean descriptionBelow
    ) {
        return new RonovaTooltipControlData(
            paletteText(
                "[ " + input + " ]",
                0xD57AD8, 0.34F, 7200L, 0.055F, false, false),
            paletteText(
                action, terminal ? 0xE43A5B : 0xE6DEE2,
                terminal ? 0.42F : 0.16F, 7600L, 0.042F, false, false),
            line(description, 0xC2B7BC, false),
            descriptionBelow
        );
    }

    private static void text(List<TooltipComponent> lines, Component text) {
        text(lines, text, 10);
    }

    private static void text(List<TooltipComponent> lines, Component text, int height) {
        lines.add(RonovaTooltipLineData.text(text, height));
    }

    private static void spacer(List<TooltipComponent> lines, int height) {
        lines.add(RonovaTooltipLineData.spacer(height));
    }

    private static void divider(List<TooltipComponent> lines, int width) {
        lines.add(RonovaTooltipLineData.divider(width));
    }

    private static MutableComponent line(String text, int rgb, boolean italic) {
        return Component.literal(text).withStyle(Style.EMPTY
            .withColor(TextColor.fromRgb(rgb))
            .withItalic(italic)
            .withBold(false));
    }

    private static Component rich(Segment... segments) {
        MutableComponent out = Component.empty();
        for (Segment segment : segments) {
            out.append(line(segment.text(), segment.rgb(), false));
        }
        return out;
    }

    private static Segment segment(String text, int rgb) {
        return new Segment(text, rgb);
    }

    private static Component gradientTitle(String text) {
        return paletteText(text, 0xE43A5B, 1.0F, 6200L, 0.070F, false, false);
    }

    private static Component paletteText(
        String text,
        int baseRgb,
        float accentStrength,
        long periodMillis,
        float characterStep,
        boolean bold,
        boolean italic
    ) {
        MutableComponent out = Component.empty();
        int[] codePoints = text.codePoints().toArray();
        float phase = (System.currentTimeMillis() % periodMillis) / (float) periodMillis;
        for (int i = 0; i < codePoints.length; i++) {
            float palettePhase = phase + i * characterStep;
            int accent = samplePalette(RONOVA_PALETTE, palettePhase);
            int rgb = mixRgb(baseRgb, accent, accentStrength);
            out.append(Component.literal(new String(Character.toChars(codePoints[i])))
                .withStyle(Style.EMPTY
                    .withColor(TextColor.fromRgb(rgb))
                    .withBold(bold)
                    .withItalic(italic)));
        }
        return out;
    }

    private static int samplePalette(int[] palette, float phase) {
        float wrapped = phase - Mth.floor(phase);
        float scaled = wrapped * palette.length;
        int left = Mth.floor(scaled) % palette.length;
        int right = (left + 1) % palette.length;
        float t = scaled - Mth.floor(scaled);
        int a = palette[left];
        int b = palette[right];
        int r = Mth.floor(Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF));
        int g = Mth.floor(Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF));
        int bl = Mth.floor(Mth.lerp(t, a & 0xFF, b & 0xFF));
        return (r << 16) | (g << 8) | bl;
    }

    private static int mixRgb(int base, int accent, float strength) {
        float amount = Mth.clamp(strength, 0.0F, 1.0F);
        int r = Mth.floor(Mth.lerp(amount, (base >> 16) & 0xFF, (accent >> 16) & 0xFF));
        int g = Mth.floor(Mth.lerp(amount, (base >> 8) & 0xFF, (accent >> 8) & 0xFF));
        int b = Mth.floor(Mth.lerp(amount, base & 0xFF, accent & 0xFF));
        return (r << 16) | (g << 8) | b;
    }

    private record Segment(String text, int rgb) {}
}
