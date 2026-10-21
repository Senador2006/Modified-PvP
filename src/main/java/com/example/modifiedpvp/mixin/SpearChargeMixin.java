package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.SpearCombat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticWeapon.class)
public class SpearChargeMixin {
	@Inject(method = "damageEntities", at = @At("HEAD"))
	private void modifiedpvp$beginCharge(ItemStack stack, int remainingUseTicks, LivingEntity user, EquipmentSlot slot, CallbackInfo callback) {
		SpearCombat.beginCharge(user, slot);
	}

	@Inject(method = "damageEntities", at = @At("RETURN"))
	private void modifiedpvp$endCharge(ItemStack stack, int remainingUseTicks, LivingEntity user, EquipmentSlot slot, CallbackInfo callback) {
		SpearCombat.endCharge(user);
	}
}
