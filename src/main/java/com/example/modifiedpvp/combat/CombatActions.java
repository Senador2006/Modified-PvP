package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.modifiedpvp.mixin.DamageAbsorbInvoker;
import com.example.modifiedpvp.network.ModNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CombatActions {
	private static boolean parryReflecting;
	private static boolean parryMitigating;

	private CombatActions() {
	}

	public static boolean mitigatingParry() {
		return parryMitigating;
	}

	public static void beginParryMitigate() {
		parryMitigating = true;
	}

	public static void endParryMitigate() {
		parryMitigating = false;
	}

	public static void tick(Player player) {
		CombatantState state = CombatantAccess.of(player);
		state.tick(player);
		if (player instanceof ServerPlayer serverPlayer && state.consumeParryResolution()) {
			ModNetworking.syncParryCooldown(serverPlayer, state.parryCooldown());
		}
		if (state.heavyCharging() && (!WeaponStats.isAxe(player.getMainHandItem()) || ModEffects.blocksAbilities(player))) {
			state.cancelHeavy();
		}
		if (state.sliding() && ModEffects.blocksMovementSkills(player)) {
			state.stopSlide();
		}
		ShieldFeatures.tick(player);
		if (state.sliding() && player instanceof ServerPlayer serverPlayer) {
			kickAlongSlide(serverPlayer, state);
			if (serverPlayer.tickCount % 3 == 0) {
				serverPlayer.level().sendParticles(ParticleTypes.END_ROD, serverPlayer.getX(), serverPlayer.getY(0.25), serverPlayer.getZ(), 1, 0.15, 0.02, 0.15, 0.0);
			}
		}
	}

	public static boolean tryDash(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.dashReady() || state.heavyLocked() || !WeaponStats.canUseMovement(player) || player.isInWater() || player.isInLava()) {
			return false;
		}
		if (state.sliding()) {
			state.stopSlide();
		}
		int dashing = ModEnchantments.level(player.level(), ModEnchantments.DASHING, player.getItemBySlot(EquipmentSlot.LEGS));
		int cooldown = CombatRules.dashCooldown(dashing);
		if (player.level().isClientSide()) {
			state.startDashCooldown(cooldown);
			return true;
		}

		Vec3 direction = WeaponStats.horizontalLook(player);
		Vec3 velocity = player.getDeltaMovement();
		double lift = player.onGround() ? Math.max(velocity.y, 0.14) : velocity.y;
		double speed = clippedDash(player, direction, CombatRules.DASH_SPEED);
		player.setDeltaMovement(direction.x * speed, lift, direction.z * speed);
		player.hurtMarked = true;
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(8);
		state.startDashCooldown(cooldown);
		exhaust(player, 0.12F);
		play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.35F);
		burst(player, ParticleTypes.CLOUD, 6);
		return true;
	}

	public static boolean tryDoubleJump(Player player) {
		CombatantState state = CombatantAccess.of(player);
		boolean airborne = !player.onGround() || player.getDeltaMovement().y > 0.08;
		if (!state.doubleJumpReady() || !airborne || !WeaponStats.canUseMovement(player) || player.isInWater() || player.isInLava() || player.onClimbable() || state.sliding()) {
			return false;
		}

		double jump = player.getAttributeValue(Attributes.JUMP_STRENGTH) + player.getJumpBoostPower();
		int jumping = ModEnchantments.level(player.level(), ModEnchantments.JUMPING, player.getItemBySlot(EquipmentSlot.FEET));
		double boost = jump * (CombatRules.DOUBLE_JUMP_MULTIPLIER + jumping * CombatRules.JUMPING_PER_LEVEL);
		if (player.level().isClientSide()) {
			state.useDoubleJump();
			return true;
		}
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, boost, velocity.z);
		player.setOnGround(false);
		player.hurtMarked = true;
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(6);
		state.useDoubleJump();
		exhaust(player, 0.08F);
		play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.55F, 1.6F);
		burst(player, ParticleTypes.CLOUD, 4);
		return true;
	}

	public static boolean tryStartSlide(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.slideReady() || state.heavyLocked() || !WeaponStats.canUseMovement(player) || !player.onGround() || player.isInWater() || !player.isSprinting()) {
			return false;
		}

		Vec3 velocity = player.getDeltaMovement();
		Vec3 horizontal = new Vec3(velocity.x, 0.0, velocity.z);
		if (horizontal.lengthSqr() < 0.01) {
			horizontal = WeaponStats.horizontalLook(player);
		}
		state.startSlide(horizontal.normalize());
		exhaust(player, 0.15F);
		play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.45F, 0.8F);
		return true;
	}

	public static void stopSlide(Player player) {
		CombatantAccess.of(player).stopSlide();
	}

	public static void cancelHeavy(Player player) {
		CombatantAccess.of(player).cancelHeavy();
	}

	public static boolean tryThrust(Player player) {
		CombatantState state = CombatantAccess.of(player);
		ItemStack sword = player.getMainHandItem();
		if (!state.thrustReady() || !player.isAlive() || player.isSpectator() || ModEffects.blocksAbilities(player) || !WeaponStats.isSword(sword)) {
			return false;
		}
		float strength = player.getAttackStrengthScale(0.5F);
		if (strength < CombatRules.THRUST_MIN_STRENGTH) {
			return false;
		}

		int rangeLevel = ModEnchantments.level(player.level(), ModEnchantments.RANGE, sword);
		if (player.level().isClientSide()) {
			player.resetAttackStrengthTicker();
			state.startThrustCooldown();
			return true;
		}
		Vec3 direction = WeaponStats.horizontalLook(player);
		double lunge = CombatRules.THRUST_LUNGE + rangeLevel * 0.05;
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(direction.x * lunge, Math.max(velocity.y, player.onGround() ? 0.08 : velocity.y), direction.z * lunge);
		player.hurtMarked = true;
		player.resetAttackStrengthTicker();
		state.startThrustCooldown();

		if (player instanceof ServerPlayer serverPlayer) {
			double range = CombatRules.THRUST_BASE_RANGE + rangeLevel * CombatRules.THRUST_RANGE_PER_LEVEL;
			LivingEntity target = thrustTarget(serverPlayer, range);
			if (target != null) {
				float damage = weaponDamage(serverPlayer, sword, target, strength, 1.2F);
				if (hurtWithWeapon(serverPlayer, target, sword, damage, EquipmentSlot.MAINHAND, 0.35)) {
					if (target instanceof Mob || target instanceof Player) {
						ComboSpecials.onThrustHit(serverPlayer);
					}
					play(serverPlayer, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
					serverPlayer.level().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
				}
			} else {
				play(serverPlayer, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.1F);
			}
			serverPlayer.swing(InteractionHand.MAIN_HAND);
			exhaust(serverPlayer, 0.25F);
		}
		return true;
	}

	public static boolean tryStartHeavy(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.heavyReady() || !player.isAlive() || player.isSpectator() || ModEffects.blocksAbilities(player) || !WeaponStats.isAxe(player.getMainHandItem())) {
			return false;
		}
		state.startHeavy(player.level().getGameTime());
		return true;
	}

	public static void releaseHeavy(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.heavyCharging()) {
			return;
		}
		int chargedTicks = state.consumeHeavy(player.level().getGameTime());
		state.startHeavyCooldown();
		player.resetAttackStrengthTicker();
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		ItemStack axe = serverPlayer.getMainHandItem();
		if (!WeaponStats.isAxe(axe) || chargedTicks < CombatRules.HEAVY_MIN_TICKS) {
			play(serverPlayer, SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 0.7F);
			return;
		}

		float ratio = Math.min(1.0F, (chargedTicks - CombatRules.HEAVY_MIN_TICKS) / (float) (CombatRules.HEAVY_FULL_TICKS - CombatRules.HEAVY_MIN_TICKS));
		int slice = ModEnchantments.level(serverPlayer.level(), ModEnchantments.SLICE, axe);
		boolean empowered = ComboSpecials.hasAxeEmpower(serverPlayer);
		List<LivingEntity> targets = new ArrayList<>(targetsInLook(serverPlayer, CombatRules.HEAVY_RANGE, 0.2));
		targets.sort(Comparator.comparingDouble(serverPlayer::distanceToSqr));
		int hits = 0;
		boolean comboHit = false;
		for (LivingEntity target : targets) {
			if (hits >= 3) {
				break;
			}
			float falloff = hits == 0 ? 1.0F : hits == 1 ? 0.55F : 0.35F;
			boolean blocking = target.isBlocking();
			boolean pvp = target instanceof Player;
			float multiplier = (0.75F + ratio * 0.85F) * (1.0F + 0.22F * slice) * falloff;
			if (empowered && (!pvp || !blocking)) {
				multiplier *= CombatRules.AXE_EMPOWER_DAMAGE;
			}
			float damage = weaponDamage(serverPlayer, axe, target, 1.0F, multiplier);
			disarmShield(serverPlayer, target, axe);
			if (empowered && pvp && blocking) {
				ControlGuard.exposeGuard(target, serverPlayer, CombatRules.AXE_EXPOSE_TICKS);
			}
			if (hurtWithWeapon(serverPlayer, target, axe, damage, EquipmentSlot.MAINHAND, 0.45 + ratio * 0.55)) {
				if (empowered && !pvp) {
					ControlGuard.stun(target, serverPlayer, CombatRules.AXE_STUN_TICKS);
				} else if (empowered && pvp) {
					ControlGuard.offBalance(target, serverPlayer, CombatRules.AXE_EXPOSE_TICKS);
				} else if (ratio > 0.65F) {
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 24, 0), serverPlayer);
				}
				hits++;
				if (target instanceof Mob || target instanceof Player) {
					comboHit = true;
				}
			}
		}

		serverPlayer.swing(InteractionHand.MAIN_HAND);
		exhaust(serverPlayer, 0.35F);
		play(serverPlayer, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.75F + ratio * 0.2F);
		serverPlayer.level().sendParticles(ParticleTypes.SWEEP_ATTACK, serverPlayer.getX(), serverPlayer.getY(0.6), serverPlayer.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		if (hits > 0) {
			if (empowered) {
				ComboSpecials.consumeAxeEmpower(serverPlayer);
			}
			ComboSpecials.onHeavySuccess(serverPlayer, comboHit);
			serverPlayer.level().sendParticles(ParticleTypes.CRIT, serverPlayer.getX(), serverPlayer.getY(0.7), serverPlayer.getZ(), 12, 0.6, 0.3, 0.6, 0.08);
		}
	}

	public static boolean tryParry(Player player) {
		CombatantState state = CombatantAccess.of(player);
		if (!state.parryReady() || !player.isAlive() || player.isSpectator() || ModEffects.blocksAbilities(player)) {
			return false;
		}
		if (player instanceof ServerPlayer serverPlayer && SpearCombat.isGuarding(serverPlayer)) {
			return false;
		}
		state.startParry();
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.swing(InteractionHand.MAIN_HAND, true);
			play(serverPlayer, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.8F);
		}
		return true;
	}

	public static float tryParryHit(ServerPlayer player, DamageSource source, float amount) {
		if (parryReflecting || parryMitigating || amount <= 0.0F || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return amount;
		}
		CombatantState state = CombatantAccess.of(player);
		if (!state.parrying()) {
			return amount;
		}
		if (ParryHitstop.holds(player)) {
			return -1.0F;
		}
		Origin origin = origin(source);
		if (origin == Origin.NONE) {
			return amount;
		}
		LivingEntity attacker = source.getEntity() instanceof LivingEntity owner ? owner
				: source.getDirectEntity() instanceof LivingEntity direct ? direct : null;
		if (origin != Origin.EXPLOSION && origin != Origin.PROJECTILE && (attacker == null || attacker == player || !attacker.isAlive())) {
			return amount;
		}
		if (attacker != null && attacker instanceof Player other && !player.canHarmPlayer(other)) {
			return amount;
		}

		boolean late = !state.parryPerfect();
		state.markParrySuccess();
		if (origin == Origin.EXPLOSION) {
			feedback(player, "combat.modifiedpvp.parry.explosion");
			play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 1.4F);
			return amount * (1.0F - CombatRules.PARRY_EXPLOSION_MITIGATION);
		}
		if (origin == Origin.PROJECTILE) {
			if (source.getDirectEntity() instanceof Projectile projectile && projectile.isAlive()) {
				projectile.discard();
			}
			feedback(player, "combat.modifiedpvp.parry.projectile");
			play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 1.6F);
			burst(player, ParticleTypes.CRIT, 8);
			return -1.0F;
		}
		if (attacker == null) {
			return amount;
		}
		if (late) {
			feedback(player, "combat.modifiedpvp.parry.late");
			play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7F, 1.1F);
			return amount * (1.0F - CombatRules.PARRY_LATE_MITIGATION);
		}

		float taken = ((DamageAbsorbInvoker) player).modifiedpvp$afterArmor(source, amount);
		taken = ((DamageAbsorbInvoker) player).modifiedpvp$afterMagic(source, taken);
		float reflected = attacker instanceof Player ? 0.0F : taken * CombatRules.PARRY_REFLECT_MULTIPLIER;
		ParryHitstop.armReflect(player, attacker, reflected);
		feedback(player, "combat.modifiedpvp.parry.melee");
		return -1.0F;
	}

	public static void finishParry(ServerPlayer player, LivingEntity attacker, float damage) {
		if (!attacker.isAlive() || attacker.isRemoved()) {
			return;
		}
		ServerLevel level = player.level();
		if (attacker instanceof Player) {
			ControlGuard.interrupt(attacker, player, CombatRules.PARRY_INTERRUPT_TICKS);
			Vec3 look = WeaponStats.horizontalLook(player);
			attacker.knockback(CombatRules.PARRY_MELEE_SHOVE, -look.x, -look.z);
		} else if (damage > 0.0F) {
			attacker.invulnerableTime = 0;
			parryReflecting = true;
			try {
				attacker.hurtServer(level, level.damageSources().playerAttack(player), damage);
			} finally {
				parryReflecting = false;
			}
			if (attacker.isAlive()) {
				ControlGuard.stun(attacker, player, CombatRules.PARRY_STUN_TICKS);
			}
		}
		player.swing(InteractionHand.MAIN_HAND, true);
		level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 12, 0.2, 0.3, 0.2, 0.15);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.6), attacker.getZ(), 8, 0.2, 0.25, 0.2, 0.05);
	}

	public static void strikeOffhand(Player player, Entity target, float attackScale) {
		if (attackScale < CombatRules.DUAL_WIELD_MIN_STRENGTH || !(target instanceof LivingEntity living) || !living.isAlive()) {
			return;
		}
		if (!WeaponStats.isDualWielding(player) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (serverPlayer.distanceToSqr(living) > squared(serverPlayer.entityInteractionRange() + 1.0)) {
			return;
		}

		ItemStack offhand = serverPlayer.getOffhandItem();
		int doubleHit = ModEnchantments.highest(serverPlayer.level(), ModEnchantments.DOUBLE_HIT, serverPlayer.getMainHandItem(), offhand);
		float multiplier = CombatRules.DUAL_WIELD_OFFHAND_RATIO * (1.0F + 0.08F * doubleHit);
		float damage = weaponDamage(serverPlayer, offhand, living, attackScale, multiplier);
		boolean hurt = hurtWithWeapon(serverPlayer, living, offhand, damage, EquipmentSlot.OFFHAND, 0.2);
		if (hurt) {
			serverPlayer.swing(InteractionHand.OFF_HAND);
			serverPlayer.level().sendParticles(ParticleTypes.ENCHANTED_HIT, living.getX(), living.getY(0.6), living.getZ(), 4, 0.15, 0.15, 0.15, 0.02);
		}
		if (living instanceof Mob || living instanceof Player) {
			ComboSpecials.onOffhandHit(serverPlayer, hurt);
		}
	}

	public static float dualWieldDelayScale(Player player) {
		if (!WeaponStats.isDualWielding(player)) {
			return 1.0F;
		}
		int level = ModEnchantments.highest(player.level(), ModEnchantments.DOUBLE_HIT, player.getMainHandItem(), player.getOffhandItem());
		float scale = 1.12F / (1.0F + 0.16F * level);
		if (ComboSpecials.dualFlurry(player)) {
			scale *= 0.72F;
		}
		return scale;
	}

	private static float weaponDamage(ServerPlayer player, ItemStack weapon, Entity target, float attackScale, float multiplier) {
		float cooldown = 0.2F + attackScale * attackScale * 0.8F;
		float damage = WeaponStats.attackDamage(weapon) * cooldown * multiplier;
		DamageSource source = player.level().damageSources().playerAttack(player);
		return EnchantmentHelper.modifyDamage(player.level(), weapon, target, source, damage);
	}

	private static boolean hurtWithWeapon(ServerPlayer player, LivingEntity target, ItemStack weapon, float damage, EquipmentSlot slot, double knockback) {
		ServerLevel level = player.level();
		DamageSource source = level.damageSources().playerAttack(player);
		target.invulnerableTime = 0;
		boolean hurt = target.hurtServer(level, source, damage);
		if (!hurt) {
			return false;
		}
		double yaw = Math.toRadians(player.getYRot());
		target.knockback(knockback, Math.sin(yaw), -Math.cos(yaw));
		EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, weapon);
		weapon.hurtAndBreak(1, player, slot);
		return true;
	}

	private static void disarmShield(ServerPlayer player, LivingEntity target, ItemStack axe) {
		if (!target.isBlocking()) {
			return;
		}
		ItemStack blocking = target.getUseItem();
		BlocksAttacks blocks = blocking.get(DataComponents.BLOCKS_ATTACKS);
		if (blocks == null) {
			return;
		}
		Weapon weapon = axe.get(DataComponents.WEAPON);
		float seconds = weapon != null ? weapon.disableBlockingForSeconds() : Weapon.AXE_DISABLES_BLOCKING_FOR_SECONDS;
		blocks.disable(player.level(), target, seconds, blocking);
	}

	private static void kickAlongSlide(ServerPlayer player, CombatantState state) {
		int kick = ModEnchantments.level(player.level(), ModEnchantments.KICK, player.getItemBySlot(EquipmentSlot.LEGS));
		if (kick <= 0) {
			return;
		}

		ServerLevel level = player.level();
		AABB box = player.getBoundingBox().inflate(0.5, 0.35, 0.5);
		double lift = CombatRules.KICK_LIFT + kick * CombatRules.KICK_LIFT_PER_LEVEL;
		double shove = CombatRules.KICK_SHOVE + kick * CombatRules.KICK_SHOVE_PER_LEVEL;
		Vec3 look = WeaponStats.horizontalLook(player);
		boolean hit = false;
		for (Entity entity : level.getEntities(player, box, candidate -> canHit(player, candidate))) {
			if (!(entity instanceof LivingEntity living) || !state.markSlideHit(living.getUUID())) {
				continue;
			}
			if (!ControlGuard.tryApply(living, ControlGuard.Kind.KICK)) {
				continue;
			}
			Vec3 toTarget = new Vec3(living.getX() - player.getX(), 0.0, living.getZ() - player.getZ());
			double dot = toTarget.lengthSqr() < 1.0E-4 ? 1.0 : toTarget.normalize().dot(look);
			double liftMul = dot <= -0.25 ? 1.15 : dot >= 0.45 ? 0.55 : 0.7;
			double shoveMul = dot >= 0.45 ? 1.15 : dot <= -0.25 ? 0.7 : 1.0;
			if (dot > -0.25 && dot < 0.45) {
				ControlGuard.offBalance(living, player, 24);
			}
			ControlGuard.mark(living, ControlGuard.Kind.KICK);
			Vec3 motion = living.getDeltaMovement();
			living.setDeltaMovement(motion.x * 0.35 + state.slideX() * shove * shoveMul, Math.max(0.0, motion.y) + lift * liftMul, motion.z * 0.35 + state.slideZ() * shove * shoveMul);
			living.setOnGround(false);
			living.hurtMarked = true;
			living.resetFallDistance();
			level.sendParticles(ParticleTypes.CLOUD, living.getX(), living.getY(0.2), living.getZ(), 8, 0.2, 0.1, 0.2, 0.02);
			hit = true;
		}
		if (hit) {
			play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8F, 0.7F + kick * 0.08F);
		}
	}

	private static LivingEntity thrustTarget(Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 end = eye.add(look.scale(range));
		AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (Entity entity : player.level().getEntities(player, area, candidate -> canHit(player, candidate))) {
			LivingEntity living = (LivingEntity) entity;
			AABB box = living.getBoundingBox().inflate(0.3 + living.getPickRadius());
			double distance = Math.sqrt(box.distanceToSqr(eye));
			if (distance > range) {
				continue;
			}
			double score;
			if (box.contains(eye) || box.clip(eye, end).isPresent()) {
				score = distance;
			} else {
				Vec3 toCenter = living.getBoundingBox().getCenter().subtract(eye);
				double minDot = distance <= CombatRules.THRUST_CLOSE_RANGE ? 0.2 : 0.72;
				if (toCenter.lengthSqr() < 1.0E-6 || toCenter.normalize().dot(look) < minDot) {
					continue;
				}
				score = distance + 1.0;
			}
			if (score < bestScore) {
				bestScore = score;
				best = living;
			}
		}
		return best;
	}

	private static List<LivingEntity> targetsInLook(Player player, double range, double minDot) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		AABB box = player.getBoundingBox().inflate(range);
		return player.level().getEntities(player, box, entity -> canHit(player, entity)).stream()
				.filter(entity -> entity instanceof LivingEntity)
				.map(entity -> (LivingEntity) entity)
				.filter(living -> {
					Vec3 toTarget = living.getBoundingBox().getCenter().subtract(eye);
					double distance = toTarget.length();
					if (distance > range || !player.hasLineOfSight(living)) {
						return false;
					}
					if (distance < 1.0E-4) {
						return true;
					}
					return toTarget.normalize().dot(look) >= minDot;
				})
				.toList();
	}

	private static boolean canHit(Player attacker, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator() || living == attacker || living.isRemoved()) {
			return false;
		}
		return !(living instanceof Player other) || attacker.canHarmPlayer(other);
	}

	private static double squared(double value) {
		return value * value;
	}

	private static double clippedDash(Player player, Vec3 direction, double speed) {
		Vec3 start = player.position().add(0.0, 0.2, 0.0);
		Vec3 end = start.add(direction.scale(speed * 4.0));
		BlockHitResult clip = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (clip.getType() == HitResult.Type.MISS) {
			return speed;
		}
		double room = start.distanceTo(clip.getLocation()) - 0.35;
		if (room <= 0.15) {
			return 0.12;
		}
		return Math.min(speed, room / 4.0);
	}

	private static Origin origin(DamageSource source) {
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return Origin.EXPLOSION;
		}
		if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
			return Origin.PROJECTILE;
		}
		if (source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()) {
			return Origin.MELEE;
		}
		return Origin.NONE;
	}

	private static void feedback(ServerPlayer player, String key) {
		player.displayClientMessage(Component.translatable(key), true);
	}

	private enum Origin {
		NONE,
		MELEE,
		PROJECTILE,
		EXPLOSION
	}

	private static void exhaust(Player player, float amount) {
		if (player instanceof ServerPlayer && !player.getAbilities().invulnerable) {
			player.causeFoodExhaustion(amount);
		}
	}

	private static void play(Player player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		if (player.level() instanceof ServerLevel level) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
		}
	}

	private static void burst(Player player, net.minecraft.core.particles.SimpleParticleType particle, int count) {
		if (player.level() instanceof ServerLevel level) {
			level.sendParticles(particle, player.getX(), player.getY(0.2), player.getZ(), count, 0.2, 0.05, 0.2, 0.01);
		}
	}
}
