package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.network.AdaptiveStatePayload;
import com.example.modifiedpvp.network.ParryCooldownPayload;
import com.example.modifiedpvp.network.ParryImpactPayload;
import com.example.modifiedpvp.network.SpearPressurePayload;
import com.example.modifiedpvp.network.SpearStatePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class ModifiedPvPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModKeybinds.register();
		MovementCooldownHud.register();
		SpearHud.register();
		AdaptiveHud.register();
		HudElementRegistry.addLast(ModifiedPvP.id("parry_impact"), ParryImpact::render);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ParryImpact.tick();
			SpearClient.tick();
			AdaptiveClient.tick();
			CombatClient.tick(client);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SpearClient.clear();
			AdaptiveClient.clear();
			ParryImpact.clear();
		});
		ClientPlayNetworking.registerGlobalReceiver(ParryImpactPayload.TYPE, (payload, context) -> ParryImpact.start(payload.ticks()));
		ClientPlayNetworking.registerGlobalReceiver(ParryCooldownPayload.TYPE, (payload, context) -> {
			if (context.player() != null) {
				CombatantAccess.of(context.player()).setParryCooldown(payload.ticks());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(SpearStatePayload.TYPE, (payload, context) -> SpearClient.applyState(
				payload.guardTicks(),
				payload.perfectTicks(),
				payload.cooldownTicks(),
				payload.cooldownMax(),
				payload.riposteTicks(),
				payload.followUpTicks(),
				payload.finisherCooldownTicks(),
				payload.selfBrokenTicks()));
		ClientPlayNetworking.registerGlobalReceiver(SpearPressurePayload.TYPE, (payload, context) -> SpearClient.applyPressure(
				payload.entityId(),
				payload.pressure(),
				payload.brokenTicks(),
				payload.enhanced(),
				payload.immune(),
				payload.style()));
		ClientPlayNetworking.registerGlobalReceiver(AdaptiveStatePayload.TYPE, (payload, context) -> AdaptiveClient.apply(
				payload.familyId(),
				payload.phaseId(),
				payload.styleId(),
				payload.progress(),
				payload.readyTicks(),
				payload.cooldownTicks(),
				payload.cooldownMax()));
	}
}
