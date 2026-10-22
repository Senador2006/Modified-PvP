package com.example.modifiedpvp.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface DamageAbsorbInvoker {
	@Invoker("getDamageAfterArmorAbsorb")
	float modifiedpvp$afterArmor(DamageSource source, float amount);

	@Invoker("getDamageAfterMagicAbsorb")
	float modifiedpvp$afterMagic(DamageSource source, float amount);
}
