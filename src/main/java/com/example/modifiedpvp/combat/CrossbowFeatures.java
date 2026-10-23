package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;
import com.example.modifiedpvp.combat.hybrid.HybridResult;

import com.example.modifiedpvp.mixin.ProjectileWeaponInvoker;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CrossbowFeatures {
	private static final ThreadLocal<ItemStack> SHOOTING = new ThreadLocal<>();
	private static final ThreadLocal<Integer> VOLLEY = new ThreadLocal<>();
	private static final ThreadLocal<UUID> SHOT = new ThreadLocal<>();
	private static final List<Grapple> GRAPPLES = new ArrayList<>();

	private CrossbowFeatures() {
	}

	public static void beginShot(ItemStack weapon) {
		SHOOTING.set(weapon);
		VOLLEY.remove();
		SHOT.set(UUID.randomUUID());
	}

	public static void endShot() {
		SHOOTING.remove();
		VOLLEY.remove();
		SHOT.remove();
	}

	public static void markProjectile(Projectile projectile) {
		ItemStack weapon = SHOOTING.get();
		if (weapon == null || weapon.isEmpty()) {
			return;
		}
		if (projectile instanceof GrappleAccess access && ModEnchantments.level(projectile.level(), ModEnchantments.GRAPPLING_HOOK, weapon) > 0) {
			access.modifiedpvp$setGrapple(true);
		}
		if (ModEnchantments.level(projectile.level(), ModEnchantments.SHOTGUN, weapon) > 0 && projectile instanceof ShotgunAccess access) {
			Integer volley = VOLLEY.get();
			if (volley == null) {
				volley = MultiHit.nextVolley();
				VOLLEY.set(volley);
			}
			access.modifiedpvp$setShotgunVolley(volley);
		}
		boolean grapple = projectile instanceof GrappleAccess access && access.modifiedpvp$isGrapple();
		if (!grapple && SHOT.get() != null) {
			HybridCombatManager.bindShot(projectile.getUUID(), SHOT.get());
		}
	}

	public static void interrupt(Player player) {
		GRAPPLES.removeIf(grapple -> grapple.player == player);
	}

	public static boolean tryLoadShotgun(LivingEntity entity, ItemStack weapon) {
		int shotgun = ModEnchantments.level(entity.level(), ModEnchantments.SHOTGUN, weapon);
		if (shotgun <= 0) {
			return false;
		}
		int count = CombatRules.SHOTGUN_BASE_PROJECTILES + shotgun - 1;
		List<ItemStack> loaded = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			ItemStack ammo = entity.getProjectile(weapon);
			if (ammo.isEmpty()) {
				break;
			}
			ItemStack used = ProjectileWeaponInvoker.modifiedpvp$useAmmo(weapon, ammo, entity, false);
			if (used.isEmpty()) {
				break;
			}
			loaded.add(used);
		}
		if (loaded.isEmpty()) {
			weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
			return true;
		}
		weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(loaded));
		return true;
	}

	public static void onFireworkBurst(Projectile projectile) {
		if (!(projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer player)) {
			return;
		}
		if (projectile instanceof GrappleAccess access && access.modifiedpvp$isGrapple()) {
			return;
		}
		if (!(projectile.level() instanceof ServerLevel level)) {
			return;
		}
		AABB area = projectile.getBoundingBox().inflate(5.0);
		LivingEntity focus = null;
		int affected = 0;
		double best = Double.MAX_VALUE;
		for (var entity : level.getEntities(projectile, area, candidate -> candidate instanceof LivingEntity living && living.isAlive() && living != player)) {
			LivingEntity living = (LivingEntity) entity;
			if (living instanceof net.minecraft.world.entity.player.Player other && !player.canHarmPlayer(other)) {
				continue;
			}
			affected++;
			double distance = living.distanceToSqr(projectile);
			if (distance < best) {
				best = distance;
				focus = living;
			}
		}
		if (affected > 0 && projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) {
			HybridResult hybrid = HybridCombatManager.onCrossbowFirework(owner, focus, affected, projectile.getUUID());
			if (!hybrid.exclusive()) {
				ComboSpecials.onFireworkSplash(owner, focus, affected, false);
			}
		}
	}

	public static void onGrappleHit(Projectile projectile, HitResult hit) {
		if (!(projectile instanceof GrappleAccess access) || !access.modifiedpvp$isGrapple()) {
			return;
		}
		if (!(projectile.level() instanceof ServerLevel level) || !(projectile.getOwner() instanceof Player player) || !player.isAlive()) {
			access.modifiedpvp$setGrapple(false);
			return;
		}
		access.modifiedpvp$setGrapple(false);
		Vec3 destination = destination(projectile, hit);
		GRAPPLES.removeIf(grapple -> grapple.player == player);
		GRAPPLES.add(new Grapple(player, destination, CombatRules.GRAPPLE_MAX_TICKS));
		level.playSound(null, destination.x, destination.y, destination.z, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 0.75F);
		level.sendParticles(ParticleTypes.CRIT, destination.x, destination.y, destination.z, 8, 0.15, 0.15, 0.15, 0.02);
	}

	public static void tickGrapples() {
		Iterator<Grapple> iterator = GRAPPLES.iterator();
		while (iterator.hasNext()) {
			Grapple grapple = iterator.next();
			Player player = grapple.player;
			if (!player.isAlive() || player.isRemoved() || player.isSpectator() || grapple.ticks <= 0) {
				iterator.remove();
				continue;
			}
			if (player.level() instanceof ServerLevel level) {
				Vec3 start = player.getEyePosition();
				if (level.clip(new ClipContext(start, grapple.destination, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
					iterator.remove();
					continue;
				}
			}
			Vec3 toTarget = grapple.destination.subtract(player.position());
			double distance = toTarget.length();
			if (distance < 1.25) {
				player.setDeltaMovement(Vec3.ZERO);
				player.hurtMarked = true;
				player.resetFallDistance();
				iterator.remove();
				continue;
			}
			player.setDeltaMovement(toTarget.scale(CombatRules.GRAPPLE_SPEED / distance));
			player.setOnGround(false);
			player.hurtMarked = true;
			player.resetFallDistance();
			grapple.ticks--;
		}
	}

	private static Vec3 destination(Projectile projectile, HitResult hit) {
		if (hit instanceof BlockHitResult blockHit) {
			Direction face = blockHit.getDirection();
			return blockHit.getLocation().add(face.getStepX() * 0.65, face.getStepY() * 0.65, face.getStepZ() * 0.65);
		}
		if (hit != null) {
			return hit.getLocation();
		}
		return projectile.position();
	}

	private static final class Grapple {
		private final Player player;
		private final Vec3 destination;
		private int ticks;

		private Grapple(Player player, Vec3 destination, int ticks) {
			this.player = player;
			this.destination = destination;
			this.ticks = ticks;
		}
	}
}
