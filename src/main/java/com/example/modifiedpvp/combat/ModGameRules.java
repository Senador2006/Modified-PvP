package com.example.modifiedpvp.combat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.adaptive.AdaptiveBalanceConfig;
import com.example.modifiedpvp.combat.adaptive.AdaptiveCombatManager;
import com.example.modifiedpvp.combat.hybrid.HybridBalanceConfig;
import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ModGameRules {
	private static boolean unlockSpecials;
	private static boolean drainWater = true;

	private ModGameRules() {
	}

	public static void register() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("modifiedpvp.properties");
		Properties properties = new Properties();
		if (Files.exists(path)) {
			try (var in = Files.newInputStream(path)) {
				properties.load(in);
			} catch (IOException exception) {
				ModifiedPvP.LOGGER.warn("Could not read modifiedpvp.properties", exception);
			}
		} else {
			properties.setProperty("unlockSpecials", "false");
			properties.setProperty("drainWater", "true");
			properties.setProperty("adaptiveDebug", "false");
			properties.setProperty("hybridCombosEnabled", "true");
			properties.setProperty("hybridRequireAdvancements", "true");
			properties.setProperty("hybridOptionalStyles", "false");
			properties.setProperty("hybridAllowFinisherOnMiss", "false");
			properties.setProperty("hybridEvidenceWindowTicks", "80");
			properties.setProperty("hybridOpportunityWindowTicks", "50");
			properties.setProperty("hybridPrimeWindowTicks", "30");
			properties.setProperty("hybridSharedCooldownTicks", "200");
			properties.setProperty("hybridSwapWindowTicks", "5");
			properties.setProperty("hybridRecoveryTicks", "4");
			properties.setProperty("hybridRecoverySpeed", "0.20");
			properties.setProperty("hybridRecoveryCap", "0.40");
			properties.setProperty("hybridMaxKnockbackBonus", "0.30");
			properties.setProperty("hybridChargeFavorTicks", "4");
			properties.setProperty("hybridGuardTicks", "8");
			properties.setProperty("hybridAdvanceSpeed", "0.40");
			properties.setProperty("pullEnabled", "true");
			properties.setProperty("pullRequireLoyalty", "true");
			properties.setProperty("pullTargetResistanceTicks", "40");
			properties.setProperty("pullStrengthLevel1", "0.16");
			properties.setProperty("pullStrengthLevel2", "0.22");
			properties.setProperty("pullStrengthLevel3", "0.28");
			properties.setProperty("pullTicksLevel1", "5");
			properties.setProperty("pullTicksLevel2", "7");
			properties.setProperty("pullTicksLevel3", "9");
			properties.setProperty("spearMaceExtendedReachEnabled", "false");
			try (var out = Files.newOutputStream(path)) {
				properties.store(out, "Modified PvP server settings");
			} catch (IOException exception) {
				ModifiedPvP.LOGGER.warn("Could not write modifiedpvp.properties", exception);
			}
		}
		unlockSpecials = Boolean.parseBoolean(properties.getProperty("unlockSpecials", "false"));
		drainWater = Boolean.parseBoolean(properties.getProperty("drainWater", "true"));
		AdaptiveCombatManager.configure(AdaptiveBalanceConfig.from(properties));
		HybridCombatManager.configure(HybridBalanceConfig.from(properties));
	}

	public static boolean unlockSpecials(ServerPlayer player) {
		return unlockSpecials;
	}

	public static boolean drainWater(Level level) {
		return drainWater;
	}
}
