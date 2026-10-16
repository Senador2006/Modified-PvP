package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CrossbowFeatures;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketMixin {
	@Inject(method = "explode", at = @At("HEAD"))
	private void modifiedpvp$grappleBurst(ServerLevel level, CallbackInfo callback) {
		CrossbowFeatures.onGrappleHit((Projectile) (Object) this, null);
	}
}
