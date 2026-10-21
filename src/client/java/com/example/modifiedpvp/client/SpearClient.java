package com.example.modifiedpvp.client;

public final class SpearClient {
	private static int guardTicks;
	private static int perfectTicks;
	private static int cooldownTicks;
	private static int cooldownMax = 1;
	private static int riposteTicks;
	private static int followUpTicks;
	private static int finisherCooldownTicks;
	private static int selfBrokenTicks;
	private static int targetId = -1;
	private static int pressure;
	private static int brokenTicks;
	private static boolean enhanced;
	private static boolean immune;

	private SpearClient() {
	}

	public static void tick() {
		if (guardTicks > 0) {
			guardTicks--;
		}
		if (perfectTicks > 0) {
			perfectTicks--;
		}
		if (cooldownTicks > 0) {
			cooldownTicks--;
		}
		if (riposteTicks > 0) {
			riposteTicks--;
		}
		if (followUpTicks > 0) {
			followUpTicks--;
		}
		if (finisherCooldownTicks > 0) {
			finisherCooldownTicks--;
		}
		if (selfBrokenTicks > 0) {
			selfBrokenTicks--;
		}
		if (brokenTicks > 0) {
			brokenTicks--;
		}
	}

	public static void applyState(int guard, int perfect, int cooldown, int max, int riposte, int followUp, int finisher, int selfBroken) {
		guardTicks = guard;
		perfectTicks = perfect;
		cooldownTicks = cooldown;
		cooldownMax = Math.max(1, max);
		riposteTicks = riposte;
		followUpTicks = followUp;
		finisherCooldownTicks = finisher;
		selfBrokenTicks = selfBroken;
	}

	public static void applyPressure(int entityId, int value, int broken, boolean enhancedBreak, boolean ruptureImmune) {
		targetId = entityId;
		pressure = value;
		brokenTicks = broken;
		enhanced = enhancedBreak;
		immune = ruptureImmune;
		if (value <= 0 && broken <= 0 && !ruptureImmune) {
			targetId = -1;
		}
	}

	public static void clear() {
		applyState(0, 0, 0, 1, 0, 0, 0, 0);
		applyPressure(-1, 0, 0, false, false);
	}

	public static boolean guarding() {
		return guardTicks > 0;
	}

	public static boolean perfect() {
		return perfectTicks > 0;
	}

	public static boolean riposteReady() {
		return riposteTicks > 0;
	}

	public static boolean followUpReady() {
		return followUpTicks > 0;
	}

	public static float cooldownReady() {
		if (guarding() || riposteReady() || followUpReady()) {
			return 1.0F;
		}
		return 1.0F - Math.min(1.0F, cooldownTicks / (float) cooldownMax);
	}

	public static int targetId() {
		return targetId;
	}

	public static int pressure() {
		return pressure;
	}

	public static int brokenTicks() {
		return brokenTicks;
	}

	public static boolean enhanced() {
		return enhanced;
	}

	public static boolean immune() {
		return immune;
	}

	public static int selfBrokenTicks() {
		return selfBrokenTicks;
	}
}
