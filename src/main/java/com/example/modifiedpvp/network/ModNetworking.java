package com.example.modifiedpvp.network;

import com.example.modifiedpvp.combat.BowFeatures;
import com.example.modifiedpvp.combat.CombatActions;
import com.example.modifiedpvp.combat.SpearCombat;
import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(CombatActionPayload.TYPE, CombatActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ParryCooldownPayload.TYPE, ParryCooldownPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ParryImpactPayload.TYPE, ParryImpactPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SpearStatePayload.TYPE, SpearStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SpearPressurePayload.TYPE, SpearPressurePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AdaptiveStatePayload.TYPE, AdaptiveStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HybridStatePayload.TYPE, HybridStatePayload.CODEC);
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

	public static void syncParryCooldown(ServerPlayer player, int ticks) {
		if (ServerPlayNetworking.canSend(player, ParryCooldownPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ParryCooldownPayload(ticks));
		}
	}

	public static void syncParryImpact(ServerPlayer player, int ticks) {
		if (ServerPlayNetworking.canSend(player, ParryImpactPayload.TYPE)) {
			ServerPlayNetworking.send(player, new ParryImpactPayload(ticks));
		}
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
			case PARRY -> CombatActions.tryParry(player);
			case SPEAR_COUNTER -> SpearCombat.tryCounter(player);
			case COMBINED_STRIKE -> CombatActions.tryCombinedStrike(player);
			case AXE_GUARD -> HybridCombatManager.tryAdvance(player);
		}
	}

	public static void syncSpearState(ServerPlayer player, int guardTicks, int perfectTicks, int cooldownTicks, int cooldownMax, int riposteTicks, int followUpTicks, int finisherCooldownTicks, int selfBrokenTicks) {
		if (ServerPlayNetworking.canSend(player, SpearStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, new SpearStatePayload(guardTicks, perfectTicks, cooldownTicks, cooldownMax, riposteTicks, followUpTicks, finisherCooldownTicks, selfBrokenTicks));
		}
	}

	public static void syncAdaptive(ServerPlayer player, AdaptiveStatePayload payload) {
		if (ServerPlayNetworking.canSend(player, AdaptiveStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncHybrid(ServerPlayer player, HybridStatePayload payload) {
		if (ServerPlayNetworking.canSend(player, HybridStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncSpearPressure(ServerPlayer player, int entityId, int pressure, int brokenTicks, boolean enhanced, boolean immune, int style) {
		if (ServerPlayNetworking.canSend(player, SpearPressurePayload.TYPE)) {
			ServerPlayNetworking.send(player, new SpearPressurePayload(entityId, pressure, brokenTicks, enhanced, immune, style));
		}
	}
}
