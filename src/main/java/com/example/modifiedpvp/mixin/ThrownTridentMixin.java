package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.TridentEffects;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public class ThrownTridentMixin {
	@Inject(method = "onHitEntity", at = @At("RETURN"))
	private void modifiedpvp$loyaltyHook(EntityHitResult hit, CallbackInfo callback) {
		TridentEffects.onHit((ThrownTrident) (Object) this, hit.getEntity());
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void modifiedpvp$drain(CallbackInfo callback) {
		TridentEffects.drainWater((ThrownTrident) (Object) this);
	}
}
