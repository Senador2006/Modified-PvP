package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.client.HybridInput;
import com.example.modifiedpvp.combat.WeaponStats;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ShieldUseMixin {
	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$axeUsesRightClick(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		if (hand == InteractionHand.OFF_HAND && WeaponStats.isAxe(player.getMainHandItem()) && WeaponStats.isShield(player.getOffhandItem()) && !HybridInput.guard()) {
			callback.setReturnValue(InteractionResult.PASS);
		}
	}
}
