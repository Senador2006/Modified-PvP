package com.example.modifiedpvp.client.mixin;

import com.example.modifiedpvp.client.ParryImpact;

import net.minecraft.client.multiplayer.ClientLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	@Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$freezeParryHitstop(CallbackInfo callback) {
		if (ParryImpact.frozen()) {
			callback.cancel();
		}
	}
}
