package com.example.modifiedpvp.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ModifiedPvPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModKeybinds.register();
		ClientTickEvents.END_CLIENT_TICK.register(CombatClient::tick);
	}
}
