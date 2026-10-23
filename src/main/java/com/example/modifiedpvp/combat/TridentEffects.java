package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

public final class TridentEffects {
	private static final List<Hook> HOOKS = new ArrayList<>();

	private TridentEffects() {
	}

	public static void onHit(ThrownTrident trident, Entity hit) {
		if (!(trident.level() instanceof ServerLevel level) || !(hit instanceof LivingEntity target) || !target.isAlive()) {
			return;
		}
		Entity owner = trident.getOwner();
		if (!(owner instanceof LivingEntity livingOwner) || owner == target || !canHook(livingOwner, target)) {
			return;
		}
		ItemStack stack = tridentStack(trident);
		int loyalty = ModEnchantments.level(level, Enchantments.LOYALTY, stack);
		if (loyalty <= 0) {
			return;
		}

		if (!ControlGuard.tryApply(target, ControlGuard.Kind.PULL)) {
			return;
		}
		int pullTicks = CombatRules.LOYALTY_PULL_BASE_TICKS + loyalty * CombatRules.LOYALTY_PULL_TICKS_PER_LEVEL;
		int stunTicks = CombatRules.LOYALTY_STUN_BASE_TICKS + loyalty * CombatRules.LOYALTY_STUN_TICKS_PER_LEVEL;
		double speed = CombatRules.LOYALTY_PULL_SPEED + loyalty * CombatRules.LOYALTY_PULL_SPEED_PER_LEVEL;
		boolean pvp = target instanceof Player;
		HOOKS.removeIf(hook -> hook.target == target || !hook.target.isAlive());
		HOOKS.add(new Hook(target, livingOwner, pullTicks, pvp ? 0 : stunTicks, speed, pvp));
		target.stopRiding();
		ControlGuard.mark(target, ControlGuard.Kind.PULL);
		if (pvp) {
			ControlGuard.offBalance(target, livingOwner, CombatRules.TRIDENT_PVP_EXPOSE_TICKS);
		} else {
			applyStun(target, livingOwner, stunTicks);
		}
		if (livingOwner instanceof ServerPlayer player) {
			boolean storm = level.isThundering();
			boolean channeling = ModEnchantments.level(level, Enchantments.CHANNELING, stack) > 0 && storm;
			ComboSpecials.onTridentThrowHit(player, target, channeling);
			ComboSpecials.onTridentPull(player, target);
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 0.65F);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 8, 0.2, 0.25, 0.2, 0.02);
	}

	public static void drainWater(ThrownTrident trident) {
		if (!(trident.level() instanceof ServerLevel level)) {
			return;
		}
		if (ModEnchantments.level(level, ModEnchantments.DRAIN, tridentStack(trident)) <= 0 || !ModGameRules.drainWater(level)) {
			return;
		}
		Vec3 from = new Vec3(trident.xo, trident.yo, trident.zo);
		Vec3 to = trident.position();
		int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) * 2.0));
		boolean removed = false;
		for (int i = 0; i <= samples; i++) {
			double delta = i / (double) samples;
			Vec3 point = from.lerp(to, delta);
			if (clearWater(level, trident.getOwner(), BlockPos.containing(point))) {
				removed = true;
			}
		}
		if (removed && trident.tickCount % 3 == 0) {
			level.playSound(null, trident.getX(), trident.getY(), trident.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.55F, 1.35F);
			level.sendParticles(ParticleTypes.SPLASH, trident.getX(), trident.getY(), trident.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
		}
	}

	public static void tick() {
		Iterator<Hook> iterator = HOOKS.iterator();
		while (iterator.hasNext()) {
			Hook hook = iterator.next();
			LivingEntity target = hook.target;
			if (!target.isAlive() || target.isRemoved() || (hook.stunTicks <= 0 && hook.pullTicks <= 0)) {
				iterator.remove();
				continue;
			}
			if (hook.pullTicks > 0 && hook.owner.isAlive() && !hook.owner.isRemoved()) {
				if (!lineOfSight(target, hook.owner)) {
					hook.pullTicks = 0;
				} else {
					pull(target, hook.owner, hook.speed);
					hook.pullTicks--;
				}
			} else if (!hook.pvpLite) {
				Vec3 motion = target.getDeltaMovement();
				target.setDeltaMovement(motion.x * 0.05, motion.y, motion.z * 0.05);
				target.hurtMarked = true;
				target.setSprinting(false);
			}
			if (target instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			if (!hook.pvpLite) {
				if (target.getEffect(MobEffects.SLOWNESS) == null) {
					applyStun(target, hook.owner, hook.stunTicks);
				}
				if (hook.stunTicks > 0) {
					hook.stunTicks--;
				}
			}
		}
	}

	public static boolean isStunned(LivingEntity entity) {
		for (Hook hook : HOOKS) {
			if (hook.target == entity && hook.stunTicks > 0) {
				return true;
			}
		}
		return false;
	}

	private static boolean lineOfSight(LivingEntity target, LivingEntity owner) {
		if (!(target.level() instanceof ServerLevel level)) {
			return false;
		}
		return level.clip(new ClipContext(target.getEyePosition(), owner.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target)).getType() == HitResult.Type.MISS;
	}

	private static void pull(LivingEntity target, LivingEntity owner, double speed) {
		Vec3 toOwner = owner.getEyePosition().subtract(target.getEyePosition());
		double distance = toOwner.length();
		if (distance < 1.35) {
			target.setDeltaMovement(Vec3.ZERO);
			target.hurtMarked = true;
			return;
		}
		target.setDeltaMovement(toOwner.scale(speed / distance));
		target.setOnGround(false);
		target.hurtMarked = true;
		target.resetFallDistance();
		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	private static void applyStun(LivingEntity target, Entity source, int ticks) {
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4, false, true, true), source);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, false, true, true), source);
		target.setSprinting(false);
	}

	private static boolean canHook(LivingEntity owner, LivingEntity target) {
		if (target.isSpectator() || !target.attackable()) {
			return false;
		}
		if (target instanceof Player player && player.getAbilities().invulnerable) {
			return false;
		}
		if (owner instanceof Player attacker && target instanceof Player victim) {
			return attacker.canHarmPlayer(victim);
		}
		return true;
	}

	private static boolean clearWater(ServerLevel level, Entity owner, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(Blocks.WATER) && !state.is(Blocks.BUBBLE_COLUMN)) {
			return false;
		}
		if (owner instanceof Player player && !level.mayInteract(player, pos)) {
			return false;
		}
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		return true;
	}

	private static ItemStack tridentStack(ThrownTrident trident) {
		ItemStack weapon = trident.getWeaponItem();
		if (weapon != null && !weapon.isEmpty()) {
			return weapon;
		}
		return trident.getPickupItemStackOrigin();
	}

	private static final class Hook {
		private final LivingEntity target;
		private final LivingEntity owner;
		private int pullTicks;
		private int stunTicks;
		private final double speed;
		private final boolean pvpLite;

		private Hook(LivingEntity target, LivingEntity owner, int pullTicks, int stunTicks, double speed, boolean pvpLite) {
			this.target = target;
			this.owner = owner;
			this.pullTicks = pullTicks;
			this.stunTicks = stunTicks;
			this.speed = speed;
			this.pvpLite = pvpLite;
		}
	}
}
