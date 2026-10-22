package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.client.ParryImpact;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
	@Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$freezeParryInput(CallbackInfo callback) {
		if (ParryImpact.frozen()) {
			callback.cancel();
		}
	}
}
