package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.TridentEffects;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityStunMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$stunBlocksJump(CallbackInfo callback) {
		if (TridentEffects.isStunned((LivingEntity) (Object) this)) {
			callback.cancel();
		}
	}
}
