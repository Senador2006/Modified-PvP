package com.example.modifiedpvp;

import com.example.modifiedpvp.combat.CombatActions;
import com.example.modifiedpvp.combat.CrossbowFeatures;
import com.example.modifiedpvp.combat.MaceChain;
import com.example.modifiedpvp.combat.TridentEffects;
import com.example.modifiedpvp.loot.ModLoot;
import com.example.modifiedpvp.network.ModNetworking;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifiedPvP implements ModInitializer {
	public static final String MOD_ID = "modifiedpvp";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModNetworking.register();
		MaceChain.register();
		ModLoot.register();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (var player : server.getPlayerList().getPlayers()) {
				CombatActions.tick(player);
			}
			TridentEffects.tick();
			CrossbowFeatures.tickGrapples();
			MaceChain.tick();
		});
		LOGGER.info("Modified PvP loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
