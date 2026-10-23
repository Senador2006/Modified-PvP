package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CombatActions;
import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;
import com.example.modifiedpvp.combat.ComboSpecials;
import com.example.modifiedpvp.combat.ModEffects;
import com.example.modifiedpvp.combat.MultiHit;
import com.example.modifiedpvp.combat.SpearCombat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerCombatMixin implements CombatantAccess {
	@Unique
	private final ArrayDeque<Boolean> modifiedpvp$hurtTracked = new ArrayDeque<>();
	@Unique
	private final CombatantState modifiedpvp$state = new CombatantState();
	@Unique
	private float modifiedpvp$attackScale;
	@Unique
	private boolean modifiedpvp$spearReapplied;

	@Override
	public CombatantState modifiedpvp$state() {
		return this.modifiedpvp$state;
	}

	@Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$slideInvulnerability(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> callback) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return;
		}
		Player player = (Player) (Object) this;
		if (!ComboSpecials.isGuarded(player)) {
			return;
		}
		callback.setReturnValue(true);
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$beginHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		if (!((Object) this instanceof ServerPlayer player)) {
			this.modifiedpvp$hurtTracked.push(false);
			return;
		}
		if (SpearCombat.reapplyingGuard()) {
			ComboSpecials.beginHurt(player);
			this.modifiedpvp$hurtTracked.push(true);
			return;
		}
		float scaled = MultiHit.incoming(player, source, amount);
		if (scaled < amount) {
			this.modifiedpvp$hurtTracked.push(false);
			if (scaled <= 0.0F) {
				callback.setReturnValue(false);
				return;
			}
			MultiHit.beginApply();
			try {
				callback.setReturnValue(player.hurtServer(level, source, scaled));
			} finally {
				MultiHit.endApply();
			}
			return;
		}
		float parried = CombatActions.tryParryHit(player, source, amount);
		if (parried < 0.0F) {
			callback.setReturnValue(false);
			this.modifiedpvp$hurtTracked.push(false);
			return;
		}
		if (parried < amount) {
			CombatActions.beginParryMitigate();
			try {
				callback.setReturnValue(player.hurtServer(level, source, parried));
			} finally {
				CombatActions.endParryMitigate();
			}
			this.modifiedpvp$hurtTracked.push(false);
			return;
		}
		float guarded = SpearCombat.guardIncoming(player, level, source, amount);
		if (guarded < 0.0F) {
			callback.setReturnValue(false);
			this.modifiedpvp$hurtTracked.push(false);
			return;
		}
		if (guarded < amount) {
			this.modifiedpvp$spearReapplied = true;
			SpearCombat.beginGuardReapply();
			try {
				callback.setReturnValue(player.hurtServer(level, source, guarded));
			} finally {
				SpearCombat.endGuardReapply();
			}
			this.modifiedpvp$hurtTracked.push(false);
			return;
		}
		ComboSpecials.beginHurt(player);
		this.modifiedpvp$hurtTracked.push(true);
	}

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void modifiedpvp$interruptCombo(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback) {
		boolean pushed = !this.modifiedpvp$hurtTracked.isEmpty() && this.modifiedpvp$hurtTracked.pop();
		if (!pushed || !((Object) this instanceof ServerPlayer player)) {
			return;
		}
		if (this.modifiedpvp$spearReapplied) {
			this.modifiedpvp$spearReapplied = false;
		}
		boolean fullyBlocked = ComboSpecials.endHurt(player);
		if (fullyBlocked || !callback.getReturnValueZ() || amount <= 0.0F) {
			return;
		}
		ComboSpecials.onDamaged(player, source);
	}

	@Inject(method = "canCriticalAttack", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$autoCrit(Entity target, CallbackInfoReturnable<Boolean> callback) {
		if (target instanceof LivingEntity living && ComboSpecials.autoCrit((Player) (Object) this, living)) {
			callback.setReturnValue(true);
		}
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

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$captureAttack(Entity target, CallbackInfo callback) {
		Player player = (Player) (Object) this;
		if (ModEffects.isStunned(player)) {
			callback.cancel();
			return;
		}
		this.modifiedpvp$attackScale = player.getAttackStrengthScale(0.5F);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void modifiedpvp$offhandStrike(Entity target, CallbackInfo callback) {
		Player player = (Player) (Object) this;
		if (this.modifiedpvp$attackScale >= CombatRules.COMBO_MIN_STRENGTH && player instanceof ServerPlayer server && target instanceof LivingEntity living
				&& (living.getLastHurtByMob() == player && living.getLastHurtByMobTimestamp() == living.tickCount)
				&& (living instanceof Mob || living instanceof Player)) {
			ComboSpecials.onMeleeHit(server, living, living.isBlocking());
		}
	}
}
