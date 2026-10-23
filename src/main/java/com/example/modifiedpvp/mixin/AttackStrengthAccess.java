package com.example.modifiedpvp.mixin;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AttackStrengthAccess {
	@Accessor("attackStrengthTicker")
	int modifiedpvp$getAttackStrengthTicker();

	@Accessor("attackStrengthTicker")
	void modifiedpvp$setAttackStrengthTicker(int ticks);
}
