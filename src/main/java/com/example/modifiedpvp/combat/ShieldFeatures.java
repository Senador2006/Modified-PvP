package com.example.modifiedpvp.combat;

import com.example.modifiedpvp.combat.adaptive.AdaptiveCombatManager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

public final class ShieldFeatures {
	private ShieldFeatures() {
	}

	public static void tick(Player player) {
	}

	public static boolean timedBlock(ServerPlayer player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.shieldRaised()) {
			return false;
		}
		long raised = state.shieldRaisedAt();
		if (raised == Long.MIN_VALUE) {
			return false;
		}
		return player.level().getGameTime() - raised <= AdaptiveCombatManager.config().shieldTimedTicks();
	}

	public static LivingEntity threatenedAlly(ServerPlayer player, DamageSource source) {
		PlayerTeam team = player.getTeam();
		if (team == null || !(player.level() instanceof ServerLevel level)) {
			return null;
		}
		Entity origin = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
		if (origin == null) {
			return null;
		}
		Vec3 from = origin instanceof LivingEntity living ? living.getEyePosition() : origin.position();
		Vec3 through = player.getBoundingBox().getCenter().subtract(from);
		if (through.lengthSqr() < 1.0E-4) {
			return null;
		}
		Vec3 direction = through.normalize();
		double behind = 3.5;
		for (ServerPlayer other : level.players()) {
			if (other == player || other.getTeam() != team || !other.isAlive() || other.isSpectator()) {
				continue;
			}
			Vec3 toAlly = other.getBoundingBox().getCenter().subtract(player.position());
			if (toAlly.lengthSqr() > behind * behind) {
				continue;
			}
			if (toAlly.normalize().dot(direction) < 0.35) {
				continue;
			}
			if (origin instanceof Projectile || player.hasLineOfSight(other)) {
				return other;
			}
		}
		return null;
	}

	public static void onBlock(ServerPlayer player, DamageSource source, boolean empowered) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player || !attacker.isAlive()) {
			return;
		}
		if (attacker instanceof Player other && !player.canHarmPlayer(other)) {
			return;
		}
		if (!ControlGuard.tryApply(attacker, ControlGuard.Kind.KNOCK)) {
			return;
		}

		ItemStack shield = player.getItemBlockingWith();
		if (shield == null || shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) {
			shield = player.getUseItem();
		}
		int push = ModEnchantments.level(level, ModEnchantments.PUSH, shield);
		double strength = CombatRules.SHIELD_PUSH_STRENGTH + push * CombatRules.SHIELD_PUSH_PER_LEVEL;
		if (empowered) {
			strength *= AdaptiveCombatManager.config().shieldRepulse();
		}
		Vec3 look = WeaponStats.horizontalLook(player);
		attacker.knockback(strength, -look.x, -look.z);
		ControlGuard.mark(attacker, ControlGuard.Kind.KNOCK);
		level.sendParticles(ParticleTypes.CLOUD, attacker.getX(), attacker.getY(0.4), attacker.getZ(), empowered ? 8 : 4, 0.15, 0.1, 0.15, 0.01);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, empowered ? 0.9F : 0.55F, empowered ? 0.7F : 0.85F + push * 0.05F);
	}
}
