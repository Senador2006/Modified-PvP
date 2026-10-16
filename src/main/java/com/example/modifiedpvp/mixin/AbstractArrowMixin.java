package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ArrowVolleyAccess;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowMixin implements ArrowVolleyAccess {
	@Unique
	private boolean modifiedpvp$volleyCompanion;

	@Override
	public void modifiedpvp$setVolleyCompanion(boolean companion) {
		this.modifiedpvp$volleyCompanion = companion;
	}

	@Override
	public boolean modifiedpvp$isVolleyCompanion() {
		return this.modifiedpvp$volleyCompanion;
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void modifiedpvp$companionIgnoresInvulnerability(EntityHitResult hit, CallbackInfo callback) {
		if (!this.modifiedpvp$volleyCompanion) {
			return;
		}
		Entity entity = hit.getEntity();
		if (entity != null) {
			entity.invulnerableTime = 0;
		}
	}
}
