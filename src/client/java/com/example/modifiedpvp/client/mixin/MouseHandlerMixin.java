package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.client.ParryImpact;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "handleAccumulatedMovement", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$freezeParryLook(CallbackInfo callback) {
		if (!ParryImpact.frozen()) {
			return;
		}
		this.accumulatedDX = 0.0;
		this.accumulatedDY = 0.0;
		callback.cancel();
	}
}
