package com.example.modifiedpvp.combat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;

public final class ShieldFeatures {
	private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

	private ShieldFeatures() {
	}

	public static void tick(Player player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		UUID id = player.getUUID();
		int cooldown = COOLDOWNS.getOrDefault(id, 0);
		if (cooldown > 0) {
			COOLDOWNS.put(id, cooldown - 1);
			return;
		}
		if (!player.isBlocking()) {
			return;
		}
		ItemStack shield = player.getUseItem();
		if (!(shield.getItem() instanceof ShieldItem)) {
			return;
		}

		Vec3 look = WeaponStats.horizontalLook(player);
		List<LivingEntity> targets = level.getEntities(player, player.getBoundingBox().inflate(CombatRules.SHIELD_PUSH_RANGE), entity -> canPush(player, entity)).stream()
				.filter(entity -> entity instanceof LivingEntity)
				.map(entity -> (LivingEntity) entity)
				.filter(living -> inFront(player, living, look))
				.sorted(Comparator.comparingDouble(player::distanceToSqr))
				.limit(4)
				.toList();
		if (targets.isEmpty()) {
			return;
		}

		int push = ModEnchantments.level(level, ModEnchantments.PUSH, shield);
		double strength = CombatRules.SHIELD_PUSH_STRENGTH + push * CombatRules.SHIELD_PUSH_PER_LEVEL;
		for (LivingEntity target : targets) {
			target.knockback(strength, -look.x, -look.z);
			level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.4), target.getZ(), 4, 0.15, 0.1, 0.15, 0.01);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.55F, 0.85F + push * 0.05F);
		COOLDOWNS.put(id, CombatRules.SHIELD_PUSH_COOLDOWN);
	}

	private static boolean inFront(Player player, LivingEntity target, Vec3 look) {
		if (player.distanceToSqr(target) > CombatRules.SHIELD_PUSH_RANGE * CombatRules.SHIELD_PUSH_RANGE) {
			return false;
		}
		Vec3 toTarget = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		Vec3 flat = new Vec3(toTarget.x, 0.0, toTarget.z);
		if (flat.lengthSqr() < 1.0E-4) {
			return true;
		}
		return flat.normalize().dot(look) >= 0.35;
	}

	private static boolean canPush(Player player, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator() || !living.attackable()) {
			return false;
		}
		if (living instanceof Player other) {
			return player.canHarmPlayer(other);
		}
		return player.getBoundingBox().inflate(CombatRules.SHIELD_PUSH_RANGE).intersects(living.getBoundingBox());
	}
}
