package com.example.modifiedpvp.combat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.adaptive.AdaptiveBalanceConfig;
import com.example.modifiedpvp.combat.adaptive.AdaptiveCombatManager;

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
			try (var out = Files.newOutputStream(path)) {
				properties.store(out, "Modified PvP server settings");
			} catch (IOException exception) {
				ModifiedPvP.LOGGER.warn("Could not write modifiedpvp.properties", exception);
			}
		}
		unlockSpecials = Boolean.parseBoolean(properties.getProperty("unlockSpecials", "false"));
		drainWater = Boolean.parseBoolean(properties.getProperty("drainWater", "true"));
		AdaptiveCombatManager.configure(AdaptiveBalanceConfig.from(properties));
	}

	public static boolean unlockSpecials(ServerPlayer player) {
		return unlockSpecials;
	}

	public static boolean drainWater(Level level) {
		return drainWater;
	}
}
