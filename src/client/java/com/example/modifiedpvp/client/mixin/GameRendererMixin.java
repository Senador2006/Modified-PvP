package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.client.ParryImpact;

import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$freezeParryView(CallbackInfo callback) {
		if (ParryImpact.frozen()) {
			callback.cancel();
		}
	}
}
