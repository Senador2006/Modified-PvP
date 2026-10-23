package com.example.modifiedpvp.client;

import com.example.modifiedpvp.combat.adaptive.AdaptiveStyle;
import com.example.modifiedpvp.combat.adaptive.FamilyPhase;
import com.example.modifiedpvp.combat.adaptive.WeaponFamily;

public final class AdaptiveClient {
	private static WeaponFamily family;
	private static FamilyPhase phase = FamilyPhase.IDLE;
	private static AdaptiveStyle style;
	private static int progress;
	private static int readyTicks;
	private static int cooldownTicks;
	private static int cooldownMax = 1;

	private AdaptiveClient() {
	}

	public static void apply(int familyId, int phaseId, int styleId, int progressValue, int ready, int cooldown, int max) {
		family = familyId >= WeaponFamily.values().length ? null : WeaponFamily.values()[familyId];
		phase = phaseId >= FamilyPhase.values().length ? FamilyPhase.IDLE : FamilyPhase.values()[phaseId];
		style = styleId >= AdaptiveStyle.values().length ? null : AdaptiveStyle.values()[styleId];
		progress = progressValue;
		readyTicks = ready;
		cooldownTicks = cooldown;
		cooldownMax = Math.max(1, max);
	}

	public static void tick() {
		if (readyTicks > 0) {
			readyTicks--;
		}
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (phase == FamilyPhase.READY && readyTicks <= 0) {
			phase = FamilyPhase.IDLE;
		}
		if (phase == FamilyPhase.COOLDOWN && cooldownTicks <= 0) {
			phase = FamilyPhase.IDLE;
		}
	}

	public static void clear() {
		family = null;
		phase = FamilyPhase.IDLE;
		style = null;
		progress = 0;
		readyTicks = 0;
		cooldownTicks = 0;
		cooldownMax = 1;
	}

	public static boolean visible() {
		return family != null && phase != FamilyPhase.IDLE;
	}

	public static WeaponFamily family() {
		return family;
	}

	public static FamilyPhase phase() {
		return phase;
	}

	public static AdaptiveStyle style() {
		return style;
	}

	public static int progress() {
		return progress;
	}

	public static int readyTicks() {
		return readyTicks;
	}

	public static int cooldownTicks() {
		return cooldownTicks;
	}

	public static int cooldownMax() {
		return cooldownMax;
	}

	public static boolean ready(WeaponFamily wanted, AdaptiveStyle expected) {
		return phase == FamilyPhase.READY && family == wanted && style == expected;
	}
}
