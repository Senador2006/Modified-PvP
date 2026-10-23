package com.example.modifiedpvp.client;

import com.example.modifiedpvp.combat.hybrid.HybridPhase;
import com.example.modifiedpvp.combat.hybrid.HybridStyleId;

public final class HybridClient {
	private static HybridStyleId style;
	private static HybridPhase phase = HybridPhase.IDLE;
	private static int cooldown;
	private static int cooldownMax = 1;
	private static boolean crown;

	private HybridClient() {
	}

	public static void apply(int styleId, int phaseId, int cooldownTicks, int max, boolean crownReady) {
		style = styleId >= HybridStyleId.values().length ? null : HybridStyleId.values()[styleId];
		phase = phaseId >= HybridPhase.values().length ? HybridPhase.IDLE : HybridPhase.values()[phaseId];
		cooldown = cooldownTicks;
		cooldownMax = Math.max(1, max);
		crown = crownReady && phase == HybridPhase.READY;
	}

	public static void tick() {
		if (phase == HybridPhase.COOLDOWN && cooldown > 0) {
			cooldown--;
			if (cooldown <= 0) {
				phase = HybridPhase.IDLE;
				style = null;
				crown = false;
			}
		}
	}

	public static void clear() {
		style = null;
		phase = HybridPhase.IDLE;
		cooldown = 0;
		cooldownMax = 1;
		crown = false;
	}

	public static boolean visible() {
		return phase == HybridPhase.READY || phase == HybridPhase.COOLDOWN;
	}

	public static HybridStyleId style() {
		return style;
	}

	public static HybridPhase phase() {
		return phase;
	}

	public static int cooldown() {
		return cooldown;
	}

	public static int cooldownMax() {
		return cooldownMax;
	}

	public static boolean crown() {
		return crown;
	}
}
