package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ModEffects;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackStunMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$stunBlocksUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		if (ModEffects.isStunned(player)) {
			callback.setReturnValue(InteractionResult.FAIL);
		}
	}
}
