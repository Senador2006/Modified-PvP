package com.example.modifiedpvp.combat.hybrid;

import java.util.Properties;

public final class HybridBalanceConfig {
	private boolean enabled = true;
	private boolean requireAdvancements = true;
	private boolean pullEnabled = true;
	private boolean debug;
	private boolean reachEnabled;
	private int pullResistance = 40;
	private double pullSpeed1 = 0.16;
	private double pullSpeed2 = 0.22;
	private double pullSpeed3 = 0.28;
	private int pullTicks1 = 5;
	private int pullTicks2 = 7;
	private int pullTicks3 = 9;
	private double recoverySpeed = 0.20;
	private double recoveryCap = 0.40;
	private int recoveryTicks = 4;
	private int chargeFavor = 4;
	private int guardTicks = 8;
	private double advanceSpeed = 0.40;
	private double knockback = 0.30;
	private int swapWindow = 5;
	private HybridBalance balance = HybridBalance.defaults();

	private HybridBalanceConfig() {
	}

	public static HybridBalanceConfig from(Properties properties) {
		HybridBalanceConfig config = new HybridBalanceConfig();
		config.enabled = readBool(properties, "hybridCombosEnabled", config.enabled);
		config.requireAdvancements = readBool(properties, "hybridRequireAdvancements", config.requireAdvancements);
		config.pullEnabled = readBool(properties, "pullEnabled", config.pullEnabled);
		config.debug = readBool(properties, "adaptiveDebug", false) || readBool(properties, "hybridDebug", false);
		config.reachEnabled = readBool(properties, "spearMaceExtendedReachEnabled", false);
		if (!readBool(properties, "pullRequireLoyalty", true)) {
			com.example.modifiedpvp.ModifiedPvP.LOGGER.warn("pullRequireLoyalty=false is ignored; Pull never activates without Loyalty");
		}
		config.pullResistance = readInt(properties, "pullTargetResistanceTicks", config.pullResistance);
		config.pullSpeed1 = readDouble(properties, "pullStrengthLevel1", config.pullSpeed1);
		config.pullSpeed2 = readDouble(properties, "pullStrengthLevel2", config.pullSpeed2);
		config.pullSpeed3 = readDouble(properties, "pullStrengthLevel3", config.pullSpeed3);
		config.pullTicks1 = readInt(properties, "pullTicksLevel1", config.pullTicks1);
		config.pullTicks2 = readInt(properties, "pullTicksLevel2", config.pullTicks2);
		config.pullTicks3 = readInt(properties, "pullTicksLevel3", config.pullTicks3);
		config.recoverySpeed = readDouble(properties, "hybridRecoverySpeed", config.recoverySpeed);
		config.recoveryCap = readDouble(properties, "hybridRecoveryCap", config.recoveryCap);
		config.recoveryTicks = readInt(properties, "hybridRecoveryTicks", config.recoveryTicks);
		config.chargeFavor = readInt(properties, "hybridChargeFavorTicks", config.chargeFavor);
		config.guardTicks = readInt(properties, "hybridGuardTicks", config.guardTicks);
		config.advanceSpeed = readDouble(properties, "hybridAdvanceSpeed", config.advanceSpeed);
		config.knockback = readDouble(properties, "hybridMaxKnockbackBonus", config.knockback);
		config.swapWindow = readInt(properties, "hybridSwapWindowTicks", config.swapWindow);
		int evidence = readInt(properties, "hybridEvidenceWindowTicks", 80);
		int opportunity = readInt(properties, "hybridOpportunityWindowTicks", 50);
		int prime = readInt(properties, "hybridPrimeWindowTicks", 30);
		int cooldown = readInt(properties, "hybridSharedCooldownTicks", 200);
		boolean optional = readBool(properties, "hybridOptionalStyles", false);
		boolean miss = readBool(properties, "hybridAllowFinisherOnMiss", false);
		config.balance = new HybridBalance(evidence, opportunity, prime, cooldown, 16, optional, miss);
		return config;
	}

	public static HybridBalanceConfig defaults() {
		return new HybridBalanceConfig();
	}

	public boolean enabled() {
		return this.enabled;
	}

	public boolean requireAdvancements() {
		return this.requireAdvancements;
	}

	public boolean pullEnabled() {
		return this.pullEnabled;
	}

	public boolean debug() {
		return this.debug;
	}

	public boolean reachEnabled() {
		return this.reachEnabled;
	}

	public int pullResistance() {
		return this.pullResistance;
	}

	public double pullSpeed(int level) {
		return switch (Math.max(1, Math.min(3, level))) {
			case 1 -> this.pullSpeed1;
			case 2 -> this.pullSpeed2;
			default -> this.pullSpeed3;
		};
	}

	public int pullTicks(int level) {
		return switch (Math.max(1, Math.min(3, level))) {
			case 1 -> this.pullTicks1;
			case 2 -> this.pullTicks2;
			default -> this.pullTicks3;
		};
	}

	public double recoverySpeed() {
		return this.recoverySpeed;
	}

	public double recoveryCap() {
		return this.recoveryCap;
	}

	public int recoveryTicks() {
		return this.recoveryTicks;
	}

	public int chargeFavor() {
		return this.chargeFavor;
	}

	public int guardTicks() {
		return this.guardTicks;
	}

	public double advanceSpeed() {
		return this.advanceSpeed;
	}

	public double knockback() {
		return this.knockback;
	}

	public int swapWindow() {
		return this.swapWindow;
	}

	public HybridBalance balance() {
		return this.balance;
	}

	private static boolean readBool(Properties properties, String key, boolean fallback) {
		return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
	}

	private static int readInt(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static double readDouble(Properties properties, String key, double fallback) {
		try {
			return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}
}
