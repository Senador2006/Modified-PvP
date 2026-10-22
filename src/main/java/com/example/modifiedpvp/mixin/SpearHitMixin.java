package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.SpearCombat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class SpearHitMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$beginSpearStab(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		LivingEntity target = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof Player player && SpearCombat.cancelCollapsedJab(player)) {
			callback.setReturnValue(false);
			return;
		}
		if (source.getEntity() instanceof Player player && SpearCombat.tryCircularCollapse(player, target)) {
			callback.setReturnValue(true);
			return;
		}
		if (source.getEntity() instanceof Player player && !SpearCombat.ignoresStabTracking()) {
			SpearCombat.beginStab(player, target);
		}
	}

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void modifiedpvp$endSpearStab(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		LivingEntity target = (LivingEntity) (Object) this;
		SpearCombat.markHurt(target, source, callback.getReturnValueZ());
		if (source.getEntity() instanceof Player player) {
			SpearCombat.endStab(player, target);
		}
	}
}
