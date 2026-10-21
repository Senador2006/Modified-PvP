package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.SpearCombat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.PiercingWeapon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiercingWeapon.class)
public class SpearJabMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void modifiedpvp$beginJab(LivingEntity attacker, EquipmentSlot slot, CallbackInfo callback) {
		SpearCombat.beginJab(attacker, slot);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void modifiedpvp$endJab(LivingEntity attacker, EquipmentSlot slot, CallbackInfo callback) {
		SpearCombat.endJab(attacker);
	}
}
