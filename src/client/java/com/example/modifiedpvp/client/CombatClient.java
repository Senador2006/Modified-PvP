package com.example.modifiedpvp.client;

import com.example.modifiedpvp.combat.BowFeatures;
import com.example.modifiedpvp.combat.CombatActions;
import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;
import com.example.modifiedpvp.combat.WeaponStats;
import com.example.modifiedpvp.network.CombatAction;
import com.example.modifiedpvp.network.CombatActionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class CombatClient {
	private static boolean wasJumpDown;
	private static boolean wasUseDown;
	private static boolean wasSlideDown;
	private static int airborneTicks;

	private CombatClient() {
	}

	public static void tick(Minecraft client) {
		Player player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			wasJumpDown = false;
			wasUseDown = false;
			wasSlideDown = false;
			airborneTicks = 0;
			return;
		}

		CombatActions.tick(player);
		trackAirtime(player);

		if (client.screen != null) {
			releaseHeldMoves(player);
			wasJumpDown = client.options.keyJump.isDown();
			wasUseDown = client.options.keyUse.isDown();
			wasSlideDown = ModKeybinds.SLIDE.isDown();
			return;
		}

		if (ModKeybinds.DASH.consumeClick() && CombatActions.tryDash(player)) {
			send(CombatAction.DASH);
		}

		boolean jumpDown = client.options.keyJump.isDown();
		if (jumpDown && !wasJumpDown && airborneTicks >= 2 && CombatActions.tryDoubleJump(player)) {
			send(CombatAction.DOUBLE_JUMP);
		}
		wasJumpDown = jumpDown;

		boolean slideDown = ModKeybinds.SLIDE.isDown();
		if (slideDown && !wasSlideDown && CombatActions.tryStartSlide(player)) {
			send(CombatAction.SLIDE_START);
		} else if (!slideDown && wasSlideDown && CombatantAccess.of(player).sliding()) {
			CombatActions.stopSlide(player);
			send(CombatAction.SLIDE_STOP);
		}
		wasSlideDown = slideDown;

		boolean useDown = client.options.keyUse.isDown();
		CombatantState state = CombatantAccess.of(player);
		if (useDown && !wasUseDown) {
			onUsePressed(client, player);
		} else if (!useDown && wasUseDown && state.heavyCharging()) {
			CombatActions.releaseHeavy(player);
			send(CombatAction.HEAVY_RELEASE);
		} else if (state.heavyCharging() && !WeaponStats.isAxe(player.getMainHandItem())) {
			CombatActions.releaseHeavy(player);
			send(CombatAction.HEAVY_RELEASE);
		}
		wasUseDown = useDown;

		if (ModKeybinds.THRUST.consumeClick() && CombatActions.tryThrust(player)) {
			send(CombatAction.THRUST);
		}

		if (ModKeybinds.PARRY.consumeClick() && CombatActions.tryParry(player)) {
			send(CombatAction.PARRY);
		}

		if (ModKeybinds.SPEAR_COUNTER.consumeClick() && WeaponStats.isSpear(player.getMainHandItem())) {
			send(CombatAction.SPEAR_COUNTER);
		}

		if (ModKeybinds.CYCLE_ARROW.consumeClick() && BowFeatures.holdingBow(player)) {
			send(CombatAction.CYCLE_ARROW);
		}

		if (state.heavyCharging()) {
			showHeavyCharge(player, level);
		}
	}

	private static void onUsePressed(Minecraft client, Player player) {
		boolean heavyOverShield = WeaponStats.isAxe(player.getMainHandItem()) && WeaponStats.isShield(player.getOffhandItem());
		if (!heavyOverShield && (player.isUsingItem() || WeaponStats.offhandConsumesUse(player))) {
			return;
		}
		if (WeaponStats.isSword(player.getMainHandItem()) && CombatActions.tryThrust(player)) {
			send(CombatAction.THRUST);
			return;
		}
		if (WeaponStats.isAxe(player.getMainHandItem()) && CombatActions.tryStartHeavy(player)) {
			send(CombatAction.HEAVY_START);
		}
	}

	private static void showHeavyCharge(Player player, ClientLevel level) {
		long elapsed = level.getGameTime() - CombatantAccess.of(player).heavyStartedAt();
		float ratio = Math.min(1.0F, Math.max(0.0F, (elapsed - CombatRules.HEAVY_MIN_TICKS) / (float) (CombatRules.HEAVY_FULL_TICKS - CombatRules.HEAVY_MIN_TICKS)));
		int filled = Math.round(ratio * 8.0F);
		String bar = "▮".repeat(filled) + "▯".repeat(Math.max(0, 8 - filled));
		player.displayClientMessage(Component.translatable("combat.modifiedpvp.heavy_charge", bar), true);
	}

	private static void releaseHeldMoves(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (state.sliding()) {
			CombatActions.stopSlide(player);
			send(CombatAction.SLIDE_STOP);
		}
		if (state.heavyCharging()) {
			CombatActions.cancelHeavy(player);
			send(CombatAction.HEAVY_CANCEL);
		}
	}

	private static void trackAirtime(Player player) {
		if (player.onGround() || player.isInWater() || player.onClimbable()) {
			airborneTicks = 0;
		} else {
			airborneTicks++;
		}
	}

	private static void send(CombatAction action) {
		if (ClientPlayNetworking.canSend(CombatActionPayload.TYPE)) {
			ClientPlayNetworking.send(new CombatActionPayload(action));
		}
	}
}
