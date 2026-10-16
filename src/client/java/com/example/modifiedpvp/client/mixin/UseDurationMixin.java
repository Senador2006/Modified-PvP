package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.combat.BowFeatures;
import com.example.modifiedpvp.combat.CombatRules;

import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UseDuration.class)
public class UseDurationMixin {
	@Inject(method = "useDuration", at = @At("RETURN"), cancellable = true)
	private static void modifiedpvp$fasterBowPull(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> callback) {
		if (entity != null && stack.getItem() instanceof BowItem && BowFeatures.hasOffhandArrows(entity)) {
			callback.setReturnValue(Math.round(callback.getReturnValue() * CombatRules.OFFHAND_DRAW_SPEED));
		}
	}
}
