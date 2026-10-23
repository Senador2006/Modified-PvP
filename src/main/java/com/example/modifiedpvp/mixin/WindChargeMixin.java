package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WindChargeItem.class)
public class WindChargeMixin {
	@Inject(method = "use", at = @At("RETURN"))
	private void modifiedpvp$wind(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		if (player instanceof ServerPlayer server && callback.getReturnValue().consumesAction()) {
			HybridCombatManager.onWind(server, true);
		}
	}
}
