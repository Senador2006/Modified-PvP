package com.example.modifiedpvp.client;

import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.network.ParryCooldownPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ModifiedPvPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModKeybinds.register();
		MovementCooldownHud.register();
		ClientTickEvents.END_CLIENT_TICK.register(CombatClient::tick);
		ClientPlayNetworking.registerGlobalReceiver(ParryCooldownPayload.TYPE, (payload, context) -> {
			if (context.player() != null) {
				CombatantAccess.of(context.player()).setParryCooldown(payload.ticks());
			}
		});
	}
}
