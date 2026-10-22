package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ComboSpecials;
import com.example.modifiedpvp.combat.ModEffects;
import com.example.modifiedpvp.combat.MultiHit;
import com.example.modifiedpvp.combat.SpearCombat;
import com.example.modifiedpvp.combat.TridentEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityStunMixin {
	@Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$stunBlocksJump(CallbackInfo callback) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (TridentEffects.isStunned(entity) || ComboSpecials.isStunned(entity) || ModEffects.isStunned(entity)) {
			callback.cancel();
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$stunnedCannotHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		if (source.getEntity() instanceof LivingEntity attacker && attacker != (Object) this && ModEffects.isStunned(attacker)) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$scaleMultiHit(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		LivingEntity target = (LivingEntity) (Object) this;
		float scaled = MultiHit.incoming(target, source, amount);
		if (scaled >= amount) {
			return;
		}
		if (scaled <= 0.0F) {
			callback.setReturnValue(false);
			return;
		}
		MultiHit.beginApply();
		try {
			callback.setReturnValue(target.hurtServer(level, source, scaled));
		} finally {
			MultiHit.endApply();
		}
	}

	@Inject(method = "applyItemBlocking", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$guardExposed(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> callback) {
		if (ModEffects.isGuardExposed((LivingEntity) (Object) this)) {
			callback.setReturnValue(0.0F);
		}
	}

	@Inject(method = "applyItemBlocking", at = @At("RETURN"))
	private void modifiedpvp$shieldBlock(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> callback) {
		LivingEntity entity = (LivingEntity) (Object) this;
		SpearCombat.markBlocked(entity, callback.getReturnValue());
		if (entity instanceof ServerPlayer player) {
			ComboSpecials.onShieldBlock(player, source, amount, callback.getReturnValue());
		}
	}
}
