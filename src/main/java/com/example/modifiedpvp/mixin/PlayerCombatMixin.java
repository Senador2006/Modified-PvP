package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CombatActions;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerCombatMixin implements CombatantAccess {
	@Unique
	private final CombatantState modifiedpvp$state = new CombatantState();
	@Unique
	private float modifiedpvp$attackScale;

	@Override
	public CombatantState modifiedpvp$state() {
		return this.modifiedpvp$state;
	}

	@Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$slideInvulnerability(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !this.modifiedpvp$state.sliding()) {
			return;
		}
		callback.setReturnValue(true);
	}

	@Inject(method = "getDesiredPose", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$slidePose(CallbackInfoReturnable<Pose> callback) {
		Player player = (Player) (Object) this;
		if (player.isSleeping() || player.isFallFlying() || player.isPassenger()) {
			return;
		}
		if (this.modifiedpvp$state.sliding()) {
			callback.setReturnValue(Pose.SWIMMING);
		}
	}

	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void modifiedpvp$dualWieldSpeed(CallbackInfoReturnable<Float> callback) {
		Player player = (Player) (Object) this;
		float scale = CombatActions.dualWieldDelayScale(player);
		if (scale != 1.0F) {
			callback.setReturnValue(callback.getReturnValue() * scale);
		}
	}

	@Inject(method = "attack", at = @At("HEAD"))
	private void modifiedpvp$captureAttack(Entity target, CallbackInfo callback) {
		this.modifiedpvp$attackScale = ((Player) (Object) this).getAttackStrengthScale(0.5F);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void modifiedpvp$offhandStrike(Entity target, CallbackInfo callback) {
		CombatActions.strikeOffhand((Player) (Object) this, target, this.modifiedpvp$attackScale);
	}
}
