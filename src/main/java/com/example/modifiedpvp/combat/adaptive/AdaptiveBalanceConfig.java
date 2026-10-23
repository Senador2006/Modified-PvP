package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumMap;
import java.util.Properties;

/**
 * Valores PvP/PvE do sistema adaptativo. Calibrar em config/modifiedpvp.properties.
 * Os números iniciais seguem as janelas da 1.2.2 e ainda serão ajustados em teste.
 */
public final class AdaptiveBalanceConfig {
	private final EnumMap<WeaponFamily, Integer> windowTicks = new EnumMap<>(WeaponFamily.class);
	private final EnumMap<WeaponFamily, Float> minEvidence = new EnumMap<>(WeaponFamily.class);
	private final EnumMap<WeaponFamily, Integer> readyTicks = new EnumMap<>(WeaponFamily.class);
	private final EnumMap<WeaponFamily, Integer> cooldownTicks = new EnumMap<>(WeaponFamily.class);
	private boolean debugLog;
	private float hitWeight = 1.0F;
	private float blockedWeight = 1.0F;
	private float perfectWeight = 1.6F;
	private float sameTargetBonus = 0.25F;
	private float varietyBonus = 0.35F;
	private float splashTargetBonus = 0.2F;
	private int maxWindowEvents = 8;
	private int shieldTimedTicks = 6;
	private float swordPierceRatio = 1.18F;
	private float swordRhythmDamage = 1.12F;
	private float dualCrossDamage = 1.15F;
	private float dualFlurrySecond = 0.35F;
	private float dualOpeningDamage = 1.10F;
	private float axeSovereignDamage = 1.35F;
	private float axeRuinDamage = 1.10F;
	private double axeSovereignKnockback = 0.55;
	private int maceReturnCooldown = 18;
	private double maceRepulseStrength = 1.35;
	private float bowDrawScale = 0.72F;
	private float bowImpact = 1.12F;
	private double bowKnockback = 0.12;
	private double crossbowScatter = 0.55;
	private double crossbowFocus = 0.75;
	private int shieldShockTicks = 16;
	private double shieldRepulse = 1.65;
	private int protectedResponseTicks = 30;
	private float tridentImpact = 1.15F;
	private double tridentShove = 0.35;
	private double spearKineticKnockback = 0.85;
	private double spearKineticMinSpeed = 0.22;
	private float extraDamagePvpCap = 6.0F;
	private double extraKnockbackCap = 0.9;

	private AdaptiveBalanceConfig() {
		this.windowTicks.put(WeaponFamily.SWORD, 40);
		this.windowTicks.put(WeaponFamily.DUAL_SWORD, 40);
		this.windowTicks.put(WeaponFamily.AXE, 100);
		this.windowTicks.put(WeaponFamily.MACE, 160);
		this.windowTicks.put(WeaponFamily.TRIDENT, 240);
		this.windowTicks.put(WeaponFamily.BOW, 80);
		this.windowTicks.put(WeaponFamily.CROSSBOW, 80);
		this.windowTicks.put(WeaponFamily.SHIELD, 100);
		this.windowTicks.put(WeaponFamily.SPEAR, 80);
		this.minEvidence.put(WeaponFamily.SWORD, 4.0F);
		this.minEvidence.put(WeaponFamily.DUAL_SWORD, 5.0F);
		this.minEvidence.put(WeaponFamily.AXE, 3.0F);
		this.minEvidence.put(WeaponFamily.MACE, 3.0F);
		this.minEvidence.put(WeaponFamily.TRIDENT, 4.0F);
		this.minEvidence.put(WeaponFamily.BOW, 5.0F);
		this.minEvidence.put(WeaponFamily.CROSSBOW, 3.0F);
		this.minEvidence.put(WeaponFamily.SHIELD, 3.0F);
		this.minEvidence.put(WeaponFamily.SPEAR, 3.0F);
		this.readyTicks.put(WeaponFamily.SWORD, 40);
		this.readyTicks.put(WeaponFamily.DUAL_SWORD, 40);
		this.readyTicks.put(WeaponFamily.AXE, 50);
		this.readyTicks.put(WeaponFamily.MACE, 50);
		this.readyTicks.put(WeaponFamily.TRIDENT, 50);
		this.readyTicks.put(WeaponFamily.BOW, 40);
		this.readyTicks.put(WeaponFamily.CROSSBOW, 40);
		this.readyTicks.put(WeaponFamily.SHIELD, 40);
		this.readyTicks.put(WeaponFamily.SPEAR, 16);
		this.cooldownTicks.put(WeaponFamily.SWORD, 60 * 20);
		this.cooldownTicks.put(WeaponFamily.DUAL_SWORD, 120 * 20);
		this.cooldownTicks.put(WeaponFamily.AXE, 50 * 20);
		this.cooldownTicks.put(WeaponFamily.MACE, 70 * 20);
		this.cooldownTicks.put(WeaponFamily.TRIDENT, 60 * 20);
		this.cooldownTicks.put(WeaponFamily.BOW, 30 * 20);
		this.cooldownTicks.put(WeaponFamily.CROSSBOW, 40 * 20);
		this.cooldownTicks.put(WeaponFamily.SHIELD, 45 * 20);
		this.cooldownTicks.put(WeaponFamily.SPEAR, 5 * 20);
	}

	public static AdaptiveBalanceConfig from(Properties properties) {
		AdaptiveBalanceConfig config = new AdaptiveBalanceConfig();
		config.debugLog = Boolean.parseBoolean(properties.getProperty("adaptiveDebug", "false"));
		config.hitWeight = readFloat(properties, "adaptive.hitWeight", config.hitWeight);
		config.blockedWeight = readFloat(properties, "adaptive.blockedWeight", config.blockedWeight);
		config.perfectWeight = readFloat(properties, "adaptive.perfectWeight", config.perfectWeight);
		config.sameTargetBonus = readFloat(properties, "adaptive.sameTargetBonus", config.sameTargetBonus);
		config.varietyBonus = readFloat(properties, "adaptive.varietyBonus", config.varietyBonus);
		config.splashTargetBonus = readFloat(properties, "adaptive.splashTargetBonus", config.splashTargetBonus);
		config.maxWindowEvents = readInt(properties, "adaptive.maxWindowEvents", config.maxWindowEvents);
		config.shieldTimedTicks = readInt(properties, "adaptive.shieldTimedTicks", config.shieldTimedTicks);
		config.swordPierceRatio = readFloat(properties, "adaptive.swordPierceRatio", config.swordPierceRatio);
		config.swordRhythmDamage = readFloat(properties, "adaptive.swordRhythmDamage", config.swordRhythmDamage);
		config.dualCrossDamage = readFloat(properties, "adaptive.dualCrossDamage", config.dualCrossDamage);
		config.dualFlurrySecond = readFloat(properties, "adaptive.dualFlurrySecond", config.dualFlurrySecond);
		config.dualOpeningDamage = readFloat(properties, "adaptive.dualOpeningDamage", config.dualOpeningDamage);
		config.axeSovereignDamage = readFloat(properties, "adaptive.axeSovereignDamage", config.axeSovereignDamage);
		config.axeRuinDamage = readFloat(properties, "adaptive.axeRuinDamage", config.axeRuinDamage);
		config.axeSovereignKnockback = readDouble(properties, "adaptive.axeSovereignKnockback", config.axeSovereignKnockback);
		config.maceReturnCooldown = readInt(properties, "adaptive.maceReturnCooldown", config.maceReturnCooldown);
		config.maceRepulseStrength = readDouble(properties, "adaptive.maceRepulseStrength", config.maceRepulseStrength);
		config.bowDrawScale = readFloat(properties, "adaptive.bowDrawScale", config.bowDrawScale);
		config.bowImpact = readFloat(properties, "adaptive.bowImpact", config.bowImpact);
		config.bowKnockback = readDouble(properties, "adaptive.bowKnockback", config.bowKnockback);
		config.crossbowScatter = readDouble(properties, "adaptive.crossbowScatter", config.crossbowScatter);
		config.crossbowFocus = readDouble(properties, "adaptive.crossbowFocus", config.crossbowFocus);
		config.shieldShockTicks = readInt(properties, "adaptive.shieldShockTicks", config.shieldShockTicks);
		config.shieldRepulse = readDouble(properties, "adaptive.shieldRepulse", config.shieldRepulse);
		config.protectedResponseTicks = readInt(properties, "adaptive.protectedResponseTicks", config.protectedResponseTicks);
		config.tridentImpact = readFloat(properties, "adaptive.tridentImpact", config.tridentImpact);
		config.tridentShove = readDouble(properties, "adaptive.tridentShove", config.tridentShove);
		config.spearKineticKnockback = readDouble(properties, "adaptive.spearKineticKnockback", config.spearKineticKnockback);
		config.spearKineticMinSpeed = readDouble(properties, "adaptive.spearKineticMinSpeed", config.spearKineticMinSpeed);
		config.extraDamagePvpCap = readFloat(properties, "adaptive.extraDamagePvpCap", config.extraDamagePvpCap);
		config.extraKnockbackCap = readDouble(properties, "adaptive.extraKnockbackCap", config.extraKnockbackCap);
		for (WeaponFamily family : WeaponFamily.values()) {
			String key = family.name().toLowerCase();
			config.windowTicks.put(family, readInt(properties, "adaptive.window." + key, config.windowTicks.get(family)));
			config.minEvidence.put(family, readFloat(properties, "adaptive.evidence." + key, config.minEvidence.get(family)));
			config.readyTicks.put(family, readInt(properties, "adaptive.ready." + key, config.readyTicks.get(family)));
			config.cooldownTicks.put(family, readInt(properties, "adaptive.cooldown." + key, config.cooldownTicks.get(family)));
		}
		return config;
	}

	public static AdaptiveBalanceConfig defaults() {
		return new AdaptiveBalanceConfig();
	}

	public boolean debugLog() {
		return this.debugLog;
	}

	public int windowTicks(WeaponFamily family) {
		return this.windowTicks.getOrDefault(family, 40);
	}

	public float minEvidence(WeaponFamily family) {
		return this.minEvidence.getOrDefault(family, 3.0F);
	}

	public int readyTicks(WeaponFamily family) {
		return this.readyTicks.getOrDefault(family, 40);
	}

	public int cooldownTicks(WeaponFamily family) {
		return this.cooldownTicks.getOrDefault(family, 40 * 20);
	}

	public float hitWeight() {
		return this.hitWeight;
	}

	public float blockedWeight() {
		return this.blockedWeight;
	}

	public float perfectWeight() {
		return this.perfectWeight;
	}

	public float sameTargetBonus() {
		return this.sameTargetBonus;
	}

	public float varietyBonus() {
		return this.varietyBonus;
	}

	public float splashTargetBonus() {
		return this.splashTargetBonus;
	}

	public int maxWindowEvents() {
		return this.maxWindowEvents;
	}

	public int shieldTimedTicks() {
		return this.shieldTimedTicks;
	}

	public float swordPierceRatio() {
		return this.swordPierceRatio;
	}

	public float swordRhythmDamage() {
		return this.swordRhythmDamage;
	}

	public float dualCrossDamage() {
		return this.dualCrossDamage;
	}

	public float dualFlurrySecond() {
		return this.dualFlurrySecond;
	}

	public float dualOpeningDamage() {
		return this.dualOpeningDamage;
	}

	public float axeSovereignDamage() {
		return this.axeSovereignDamage;
	}

	public float axeRuinDamage() {
		return this.axeRuinDamage;
	}

	public double axeSovereignKnockback() {
		return this.axeSovereignKnockback;
	}

	public int maceReturnCooldown() {
		return this.maceReturnCooldown;
	}

	public double maceRepulseStrength() {
		return this.maceRepulseStrength;
	}

	public float bowDrawScale() {
		return this.bowDrawScale;
	}

	public float bowImpact() {
		return this.bowImpact;
	}

	public double bowKnockback() {
		return this.bowKnockback;
	}

	public double crossbowScatter() {
		return this.crossbowScatter;
	}

	public double crossbowFocus() {
		return this.crossbowFocus;
	}

	public int shieldShockTicks() {
		return this.shieldShockTicks;
	}

	public double shieldRepulse() {
		return this.shieldRepulse;
	}

	public int protectedResponseTicks() {
		return this.protectedResponseTicks;
	}

	public float tridentImpact() {
		return this.tridentImpact;
	}

	public double tridentShove() {
		return this.tridentShove;
	}

	public double spearKineticKnockback() {
		return this.spearKineticKnockback;
	}

	public double spearKineticMinSpeed() {
		return this.spearKineticMinSpeed;
	}

	public float extraDamagePvpCap() {
		return this.extraDamagePvpCap;
	}

	public double extraKnockbackCap() {
		return this.extraKnockbackCap;
	}

	private static int readInt(Properties properties, String key, int fallback) {
		try {
			return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static float readFloat(Properties properties, String key, float fallback) {
		try {
			return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
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
