package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ComboSpecials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MaceItem.class)
public class MaceItemMixin {
	@Inject(method = "hurtEnemy", at = @At("HEAD"))
	private void modifiedpvp$maceCombo(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfo callback) {
		if (attacker instanceof ServerPlayer player) {
			ComboSpecials.onMaceMelee(player, target);
		}
	}
}
