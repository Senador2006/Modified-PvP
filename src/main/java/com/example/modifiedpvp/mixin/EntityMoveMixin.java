package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ParryHitstop;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMoveMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$freezeParryHitstop(MoverType type, Vec3 movement, CallbackInfo callback) {
		Entity self = (Entity) (Object) this;
		if (!ParryHitstop.holds(self)) {
			return;
		}
		self.setDeltaMovement(Vec3.ZERO);
		callback.cancel();
	}
}
