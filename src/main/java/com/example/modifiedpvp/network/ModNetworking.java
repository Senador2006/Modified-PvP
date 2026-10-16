package com.example.modifiedpvp.network;

import com.example.modifiedpvp.combat.BowFeatures;
import com.example.modifiedpvp.combat.CombatActions;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(CombatActionPayload.TYPE, CombatActionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CombatActionPayload.TYPE, (payload, context) -> {
			CombatAction action = payload.action();
			if (action == null) {
				return;
			}
			ServerPlayer player = context.player();
			context.server().execute(() -> handle(player, action));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player != null) {
				CombatActions.stopSlide(handler.player);
			}
		});
	}

	private static void handle(ServerPlayer player, CombatAction action) {
		switch (action) {
			case DASH -> CombatActions.tryDash(player);
			case DOUBLE_JUMP -> CombatActions.tryDoubleJump(player);
			case SLIDE_START -> CombatActions.tryStartSlide(player);
			case SLIDE_STOP -> CombatActions.stopSlide(player);
			case THRUST -> CombatActions.tryThrust(player);
			case HEAVY_START -> CombatActions.tryStartHeavy(player);
			case HEAVY_RELEASE -> CombatActions.releaseHeavy(player);
			case HEAVY_CANCEL -> CombatActions.cancelHeavy(player);
			case CYCLE_ARROW -> BowFeatures.cycleBundleArrow(player);
		}
	}
}
