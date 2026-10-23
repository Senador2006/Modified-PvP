package com.example.modifiedpvp.combat.hybrid;

import com.example.modifiedpvp.combat.ControlGuard;
import com.example.modifiedpvp.combat.WeaponStats;
import com.example.modifiedpvp.mixin.AttackStrengthAccess;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HybridRewards {
	private HybridRewards() {
	}

	public static void apply(ServerPlayer player, LivingEntity target, HybridStyleId style, HybridFact fact, HybridBalanceConfig config) {
		switch (style) {
			case CHARGE_IMPACT, AERIAL_REACTION, GUIDED_FALL -> glide(player, config.recoverySpeed(), config.recoveryCap());
			case EXTENDED_REACH, THUNDER_ECHO -> {
			}
			case RELENTLESS_HUNT, OFFENSIVE_RETURN, SOVEREIGN_RETALIATION, IMMEDIATE_RESPONSE, ARMED_FEINT -> recover(player, config.recoveryTicks());
			case KING_PRESSURE -> {
				recover(player, config.recoveryTicks());
				crown(player);
			}
			case CROWN_OPENING -> {
				recover(player, config.recoveryTicks());
				crown(player);
			}
			case ANCHOR_IMPACT, SUDDEN_SHOT -> shove(player, target, config.knockback());
			case LAST_WORD -> {
				if (fact.action() == HybridAction.CROSSBOW_FIREWORK) {
					step(player, -config.recoverySpeed());
				} else {
					shove(player, target, config.knockback());
				}
				crown(player);
			}
			case FORMATION_BREAK, GUARD_CUT -> shoveSide(player, target, config.knockback());
			case PROTECTED_THRUST -> step(player, -config.recoverySpeed());
			case BASTION_ADVANCE -> advance(player, config.advanceSpeed());
			case COORDINATED_DUEL, LAST_RESORT -> recover(player, Math.max(1, config.recoveryTicks() - 1));
		}
	}

	public static void recover(ServerPlayer player, int ticks) {
		if (ticks <= 0) {
			return;
		}
		int delay = Math.max(1, Math.round(player.getCurrentItemAttackStrengthDelay()));
		AttackStrengthAccess access = (AttackStrengthAccess) player;
		int current = access.modifiedpvp$getAttackStrengthTicker();
		access.modifiedpvp$setAttackStrengthTicker(Math.min(delay, current + ticks));
	}

	public static void glide(ServerPlayer player, double speed, double cap) {
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 motion = player.getDeltaMovement();
		double x = motion.x + look.x * speed;
		double z = motion.z + look.z * speed;
		double horizontal = Math.sqrt(x * x + z * z);
		if (horizontal > cap && horizontal > 1.0E-4) {
			double scale = cap / horizontal;
			x *= scale;
			z *= scale;
		}
		player.setDeltaMovement(x, motion.y, z);
		player.hurtMarked = true;
	}

	public static void advance(ServerPlayer player, double speed) {
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(look.x * speed, Math.max(motion.y, 0.02), look.z * speed);
		player.hurtMarked = true;
	}

	public static void step(ServerPlayer player, double speed) {
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x + look.x * speed, motion.y, motion.z + look.z * speed);
		player.hurtMarked = true;
	}

	public static void shove(ServerPlayer player, LivingEntity target, double strength) {
		if (target == null || !target.isAlive() || strength <= 0.0) {
			return;
		}
		if (target instanceof Player && !ControlGuard.tryApply(target, ControlGuard.Kind.KNOCK)) {
			return;
		}
		Vec3 look = WeaponStats.horizontalLook(player);
		target.knockback(strength, -look.x, -look.z);
		target.hurtMarked = true;
		if (target instanceof Player) {
			ControlGuard.mark(target, ControlGuard.Kind.KNOCK);
		}
	}

	public static void shoveSide(ServerPlayer player, LivingEntity target, double strength) {
		if (target == null || !target.isAlive() || strength <= 0.0) {
			return;
		}
		if (target instanceof Player && !ControlGuard.tryApply(target, ControlGuard.Kind.KNOCK)) {
			return;
		}
		Vec3 look = WeaponStats.horizontalLook(player);
		target.knockback(strength, look.z, -look.x);
		target.hurtMarked = true;
		if (target instanceof Player) {
			ControlGuard.mark(target, ControlGuard.Kind.KNOCK);
		}
	}

	private static void crown(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.35F, 0.85F);
		level.sendParticles(ParticleTypes.CRIMSON_SPORE, player.getX(), player.getY(0.8), player.getZ(), 6, 0.2, 0.25, 0.2, 0.01);
	}
}
