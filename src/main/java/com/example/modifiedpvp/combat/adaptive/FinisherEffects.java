package com.example.modifiedpvp.combat.adaptive;

import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.ControlGuard;
import com.example.modifiedpvp.combat.WeaponStats;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FinisherEffects {
	private static final ThreadLocal<Boolean> GENERATED = ThreadLocal.withInitial(() -> false);

	private FinisherEffects() {
	}

	public static boolean generated() {
		return Boolean.TRUE.equals(GENERATED.get());
	}

	public static void beginGenerated() {
		GENERATED.set(true);
	}

	public static void endGenerated() {
		GENERATED.set(false);
	}

	public static void cue(ServerPlayer player, AdaptiveStyle style) {
		player.displayClientMessage(Component.translatable(style.readyKey()), true);
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.85F, 0.8F);
			level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(0.7), player.getZ(), 10, 0.3, 0.3, 0.3, 0.08);
		}
	}

	public static void feedback(ServerPlayer player, String key) {
		player.displayClientMessage(Component.translatable(key), true);
	}

	public static void burst(LivingEntity target, int count) {
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), count, 0.2, 0.25, 0.2, 0.08);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.05F);
		}
	}

	public static void shove(ServerPlayer player, LivingEntity target, double strength, AdaptiveBalanceConfig config) {
		if (target == null || !target.isAlive()) {
			return;
		}
		if (target instanceof Player && !ControlGuard.tryApply(target, ControlGuard.Kind.KNOCK)) {
			return;
		}
		double capped = Math.min(strength, config.extraKnockbackCap());
		Vec3 look = WeaponStats.horizontalLook(player);
		target.knockback(capped, -look.x, -look.z);
		if (target instanceof Player) {
			ControlGuard.mark(target, ControlGuard.Kind.KNOCK);
		}
	}

	public static void interrupt(ServerPlayer player, LivingEntity target, int ticks) {
		if (target instanceof Player) {
			ControlGuard.interrupt(target, player, ticks);
		} else {
			ControlGuard.offBalance(target, player, ticks);
		}
	}

	public static float capExtra(float extra, LivingEntity target, AdaptiveBalanceConfig config) {
		if (extra <= 0.0F) {
			return 0.0F;
		}
		if (target instanceof Player) {
			return Math.min(extra, config.extraDamagePvpCap());
		}
		return extra;
	}

	public static boolean pvp(LivingEntity target) {
		return target instanceof Player;
	}

	public static int controlTicks() {
		return CombatRules.CONTROL_RESIST_TICKS;
	}
}
