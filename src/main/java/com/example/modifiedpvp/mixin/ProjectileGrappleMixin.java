package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CrossbowFeatures;
import com.example.modifiedpvp.combat.GrappleAccess;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class ProjectileGrappleMixin implements GrappleAccess {
	@Unique
	private boolean modifiedpvp$grapple;

	@Override
	public void modifiedpvp$setGrapple(boolean grapple) {
		this.modifiedpvp$grapple = grapple;
	}

	@Override
	public boolean modifiedpvp$isGrapple() {
		return this.modifiedpvp$grapple;
	}

	@Inject(method = "onHit", at = @At("HEAD"))
	private void modifiedpvp$grappleHit(HitResult hit, CallbackInfo callback) {
		CrossbowFeatures.onGrappleHit((Projectile) (Object) this, hit);
	}
}
