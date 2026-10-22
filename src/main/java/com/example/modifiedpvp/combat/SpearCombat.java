package com.example.modifiedpvp.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.network.ModNetworking;
import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpearCombat {
	private static final AttachmentType<Integer> PERFECT_COUNTERS = AttachmentRegistry.<Integer>create(ModifiedPvP.id("spear_perfect_counters"), builder -> builder
			.persistent(Codec.INT)
			.copyOnDeath());
	private static final String ADVANCEMENT = "combat/spear";
	private static final double GUARD_MIN_DOT = Math.cos(Math.toRadians(SpearRules.GUARD_CONE_DEGREES / 2.0));

	private static final Map<UUID, GuardState> STATES = new HashMap<>();
	private static final Map<UUID, Integer> CIRCULAR_SPINS = new HashMap<>();
	private static JabContext jab;
	private static ServerPlayer charging;
	private static StabFrame frame;
	private static boolean reapplyingGuard;
	private static boolean autoRiposting;

	private SpearCombat() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> onGone(entity));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onGone(player));
		ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((original, moved, origin, destination) -> {
			if (original instanceof LivingEntity living) {
				SpearPressure.forgetTarget(destination.getServer(), living.getUUID());
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (handler.player != null) {
				syncState(handler.player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player != null) {
				STATES.remove(handler.player.getUUID());
				CIRCULAR_SPINS.remove(handler.player.getUUID());
				SpearPressure.forgetTarget(server, handler.player.getUUID());
				SpearPressure.forget(handler.player.getUUID());
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STATES.clear();
			SpearPressure.clear();
			jab = null;
			charging = null;
			frame = null;
			reapplyingGuard = false;
			autoRiposting = false;
			CIRCULAR_SPINS.clear();
		});
	}

	public static boolean reapplyingGuard() {
		return reapplyingGuard;
	}

	public static boolean ignoresStabTracking() {
		return autoRiposting;
	}

	public static boolean cancelCollapsedJab(Player player) {
		return jab != null && jab.player == player && jab.collapsed && !autoRiposting;
	}

	public static void beginGuardReapply() {
		reapplyingGuard = true;
	}

	public static void endGuardReapply() {
		reapplyingGuard = false;
	}

	public static void tick(MinecraftServer server) {
		SpearPressure.tick(server);
		tickSpins(server);
		if (STATES.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, GuardState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, GuardState> entry = iterator.next();
			GuardState state = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				iterator.remove();
				continue;
			}
			if (now < state.guardUntil && !canHoldGuard(player)) {
				state.guardUntil = now;
				syncState(player);
			}
			if (state.idle(now)) {
				iterator.remove();
			}
		}
	}

	public static boolean isGuarding(ServerPlayer player) {
		GuardState state = STATES.get(player.getUUID());
		return state != null && player.level().getGameTime() < state.guardUntil;
	}

	public static boolean tryCounter(ServerPlayer player) {
		if (!WeaponStats.isSpear(player.getMainHandItem())) {
			return false;
		}
		long now = player.level().getGameTime();
		GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
		if (tryFinisher(player, state, now)) {
			return true;
		}
		if (now < state.cooldownUntil || now < state.guardUntil || !canHoldGuard(player) || CombatantAccess.of(player).parrying() || player.isUsingItem()) {
			return false;
		}

		state.guardUntil = now + SpearRules.GUARD_TOTAL_TICKS;
		state.perfectUntil = now + SpearRules.GUARD_PERFECT_TICKS;
		state.perfectUsed = false;
		state.perfectAttacker = null;
		state.cooldownUntil = now + SpearRules.GUARD_TOTAL_TICKS + SpearRules.GUARD_COOLDOWN_TICKS;
		state.riposteUntil = 0L;
		state.riposteTarget = null;

		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPEAR_USE, SoundSource.PLAYERS, 0.6F, 1.3F);
		Vec3 front = player.getEyePosition().add(WeaponStats.horizontalLook(player).scale(0.7));
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, front.x, front.y - 0.35, front.z, 5, 0.2, 0.2, 0.2, 0.0);
		syncState(player);
		return true;
	}

	public static float guardIncoming(ServerPlayer player, ServerLevel level, DamageSource source, float amount) {
		if (reapplyingGuard || autoRiposting) {
			return amount;
		}
		GuardState state = STATES.get(player.getUUID());
		if (state == null || amount <= 0.0F) {
			return amount;
		}
		long now = level.getGameTime();
		if (!(source.getEntity() instanceof LivingEntity attacker) || !isGuardableMelee(source, attacker, player)) {
			return amount;
		}
		if (state.perfectUsed && attacker.getUUID().equals(state.perfectAttacker) && now == state.perfectTick) {
			return -1.0F;
		}
		if (now >= state.guardUntil || !WeaponStats.isSpear(player.getMainHandItem())) {
			return amount;
		}
		if (player.invulnerableTime > 10 || player.isInvulnerableTo(level, source)) {
			return amount;
		}
		if (!inFront(player, attacker) || !player.hasLineOfSight(attacker)) {
			return amount;
		}
		if (now < state.perfectUntil && !state.perfectUsed) {
			if (ParryHitstop.holds(player)) {
				return -1.0F;
			}
			state.perfectUsed = true;
			state.perfectTick = now;
			state.perfectAttacker = attacker.getUUID();
			state.guardUntil = now;
			state.riposteUntil = 0L;
			state.riposteTarget = null;
			state.followUpUntil = 0L;
			state.followUpTarget = null;
			countPerfect(player, attacker);
			ParryHitstop.armSpear(player, attacker);
			syncState(player);
			return -1.0F;
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.5F, 0.9F);
		return amount * (1.0F - SpearRules.GUARD_DAMAGE_REDUCTION);
	}

	public static void beginJab(LivingEntity attacker, EquipmentSlot slot) {
		if (!(attacker instanceof ServerPlayer player) || slot != EquipmentSlot.MAINHAND || !WeaponStats.isSpear(player.getMainHandItem())) {
			return;
		}
		long now = player.level().getGameTime();
		Role role = Role.NONE;
		UUID roleTarget = null;
		boolean guardCut = false;
		GuardState state = STATES.get(player.getUUID());
		if (state != null) {
			if (now < state.riposteUntil) {
				role = Role.RIPOSTE;
				roleTarget = state.riposteTarget;
			} else if (now < state.followUpUntil) {
				role = Role.FOLLOW_UP;
				roleTarget = state.followUpTarget;
			}
			if (now < state.guardUntil) {
				state.guardUntil = now;
				guardCut = true;
			}
		}
		jab = new JabContext(player, player.getAttackStrengthScale(0.5F), role, roleTarget, romperLevel(player), guardCut);
	}

	public static void endJab(LivingEntity attacker) {
		JabContext context = jab;
		if (context == null || context.player != attacker) {
			return;
		}
		jab = null;
		GuardState state = STATES.get(context.player.getUUID());
		if (state == null || (context.role == Role.NONE && !context.guardCut)) {
			return;
		}
		long now = context.player.level().getGameTime();
		if (context.role == Role.RIPOSTE) {
			state.riposteUntil = 0L;
			state.riposteTarget = null;
			if (context.roleHit && context.romper >= 2) {
				state.followUpUntil = now + SpearRules.ROMPER_FOLLOW_UP_TICKS;
				state.followUpTarget = context.roleTarget;
			} else if (!context.roleHit) {
				ServerLevel level = context.player.level();
				level.playSound(null, context.player.getX(), context.player.getY(), context.player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.6F, 0.8F);
			}
		} else if (context.role == Role.FOLLOW_UP) {
			state.followUpUntil = 0L;
			state.followUpTarget = null;
		}
		syncState(context.player);
	}

	public static void beginCharge(LivingEntity attacker, EquipmentSlot slot) {
		if (!(attacker instanceof ServerPlayer player) || slot != EquipmentSlot.MAINHAND || !WeaponStats.isSpear(player.getMainHandItem())) {
			return;
		}
		charging = player;
		GuardState state = STATES.get(player.getUUID());
		if (state != null && player.level().getGameTime() < state.guardUntil) {
			state.guardUntil = player.level().getGameTime();
			syncState(player);
		}
	}

	public static void endCharge(LivingEntity attacker) {
		if (charging == attacker) {
			charging = null;
		}
	}

	public static boolean tryCircularCollapse(Player self, LivingEntity target) {
		if (autoRiposting || !(self instanceof ServerPlayer player) || jab == null || jab.player != player) {
			return false;
		}
		if (jab.strength < SpearRules.JAB_MIN_STRENGTH || !WeaponStats.isSpear(player.getMainHandItem())) {
			return false;
		}
		long now = player.level().getGameTime();
		SpearPressure.Opening opening = SpearPressure.openingOn(player, target, now);
		if (opening == null) {
			return false;
		}
		if (!unlocked(player)) {
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.circular_locked"), true);
			return false;
		}
		GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
		if (now < state.finisherCooldownUntil) {
			return false;
		}
		jab.collapsed = true;
		boolean enhanced = opening.enhanced && romperLevel(player) >= 3;
		if (!circularStrike(player, target, enhanced, opening.category)) {
			jab.collapsed = false;
			return false;
		}
		SpearPressure.consume(player, opening, now);
		state.finisherCooldownUntil = now + SpearRules.FINISHER_COOLDOWN_TICKS;
		syncState(player);
		return true;
	}

	public static void beginStab(Player self, Entity target) {
		if (!(self instanceof ServerPlayer player) || !(target instanceof LivingEntity living)) {
			return;
		}
		Kind kind;
		if (jab != null && jab.player == player) {
			kind = Kind.JAB;
		} else if (charging == player) {
			kind = Kind.CHARGE;
		} else {
			return;
		}
		frame = new StabFrame(player, living, kind, frame);
	}

	public static void endStab(Player self, LivingEntity target) {
		StabFrame current = frame;
		if (current == null || current.player != self || current.target != target) {
			return;
		}
		frame = current.previous;
		if (!current.damaged && !current.blocked) {
			return;
		}
		ServerPlayer player = current.player;
		int romper = romperLevel(player);
		if (current.kind == Kind.CHARGE) {
			SpearPressure.add(player, target, current.blocked ? SpearRules.PRESSURE_SHIELD : SpearRules.PRESSURE_CHARGE,
					current.blocked ? SpearPressure.Cause.SHIELD : SpearPressure.Cause.CHARGE, romper);
			return;
		}

		JabContext context = jab;
		boolean roleTarget = context != null && context.roleTarget != null && context.roleTarget.equals(target.getUUID());
		if (current.blocked) {
			SpearPressure.add(player, target, SpearRules.PRESSURE_SHIELD, SpearPressure.Cause.SHIELD, romper);
		} else if (roleTarget && context.role == Role.RIPOSTE) {
			context.roleHit = true;
			float points = SpearRules.PRESSURE_RIPOSTE + (romper >= 1 ? SpearRules.PRESSURE_ROMPER_RIPOSTE : 0.0F);
			ServerLevel level = player.level();
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 1.25F);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.25, 0.25, 0.25, 0.15);
			SpearPressure.add(player, target, points, SpearPressure.Cause.RIPOSTE, romper);
		} else if (roleTarget && context.role == Role.FOLLOW_UP && romper >= 2) {
			context.roleHit = true;
			player.level().sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
			SpearPressure.add(player, target, SpearRules.PRESSURE_ROMPER_FOLLOW_UP, SpearPressure.Cause.FOLLOW_UP, romper);
		} else if (context != null && context.strength >= SpearRules.JAB_MIN_STRENGTH) {
			float points = effectiveRange(player, target) ? SpearRules.PRESSURE_JAB_EFFECTIVE : SpearRules.PRESSURE_JAB;
			SpearPressure.add(player, target, points, SpearPressure.Cause.JAB, romper);
		}
	}

	public static void markHurt(LivingEntity target, DamageSource source, boolean hurt) {
		StabFrame current = frame;
		if (hurt && current != null && current.target == target && source.getEntity() == current.player) {
			current.damaged = true;
		}
	}

	public static void markBlocked(LivingEntity target, float blocked) {
		StabFrame current = frame;
		if (blocked > 0.0F && current != null && current.target == target) {
			current.blocked = true;
		}
	}

	public static void syncState(ServerPlayer player) {
		long now = player.level().getGameTime();
		GuardState state = STATES.get(player.getUUID());
		int selfBroken = SpearPressure.brokenTicksOn(player, now);
		if (state == null) {
			ModNetworking.syncSpearState(player, 0, 0, 0, 1, 0, 0, 0, selfBroken);
			return;
		}
		ModNetworking.syncSpearState(player,
				remaining(state.guardUntil, now),
				state.perfectUsed ? 0 : remaining(Math.min(state.perfectUntil, state.guardUntil), now),
				remaining(state.cooldownUntil, now),
				SpearRules.GUARD_TOTAL_TICKS + SpearRules.GUARD_COOLDOWN_TICKS,
				remaining(state.riposteUntil, now),
				remaining(state.followUpUntil, now),
				remaining(state.finisherCooldownUntil, now),
				selfBroken);
	}

	private static boolean tryFinisher(ServerPlayer player, GuardState state, long now) {
		if (!canHoldGuard(player) || CombatantAccess.of(player).parrying()) {
			return false;
		}
		SpearPressure.Opening opening = SpearPressure.openingAimed(player, now);
		if (opening == null) {
			return false;
		}
		if (!unlocked(player)) {
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.finisher_locked"), true);
			return false;
		}
		if (now < state.finisherCooldownUntil) {
			int seconds = (int) Math.ceil((state.finisherCooldownUntil - now) / 20.0);
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.finisher_cooldown", seconds), true);
			return false;
		}
		boolean enhanced = opening.enhanced && romperLevel(player) >= 3;
		if (!strike(player, opening.target, enhanced, opening.category)) {
			return true;
		}
		SpearPressure.consume(player, opening, now);
		state.finisherCooldownUntil = now + SpearRules.FINISHER_COOLDOWN_TICKS;
		syncState(player);
		return true;
	}

	public static void resolvePerfectRiposte(ServerPlayer player, LivingEntity attacker) {
		if (!attacker.isAlive() || attacker.isRemoved()) {
			return;
		}
		instantDoubleThrust(player, attacker);
		syncState(player);
	}

	private static void instantDoubleThrust(ServerPlayer player, LivingEntity attacker) {
		int romper = romperLevel(player);
		boolean first = riposteThrust(player, attacker, 0.35);
		if (first) {
			float points = SpearRules.PRESSURE_RIPOSTE + (romper >= 1 ? SpearRules.PRESSURE_ROMPER_RIPOSTE : 0.0F);
			SpearPressure.add(player, attacker, points, SpearPressure.Cause.RIPOSTE, romper);
		}
		if (attacker.isAlive() && !attacker.isRemoved()) {
			boolean second = riposteThrust(player, attacker, 0.28);
			if (second && romper >= 2) {
				SpearPressure.add(player, attacker, SpearRules.PRESSURE_ROMPER_FOLLOW_UP, SpearPressure.Cause.FOLLOW_UP, romper);
			}
		}
		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();
		if (!player.getAbilities().invulnerable) {
			player.causeFoodExhaustion(0.2F);
		}
	}

	private static boolean riposteThrust(ServerPlayer player, LivingEntity target, double knockback) {
		if (!target.isAlive() || target.isRemoved() || !WeaponStats.isSpear(player.getMainHandItem())) {
			return false;
		}
		if (target instanceof Player other && !player.canHarmPlayer(other)) {
			return false;
		}
		ServerLevel level = player.level();
		ItemStack spear = player.getMainHandItem();
		DamageSource source = level.damageSources().playerAttack(player);
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float damage = EnchantmentHelper.modifyDamage(level, spear, target, source, base * SpearRules.RIPOSTE_THRUST_MULTIPLIER);
		target.invulnerableTime = 0;
		autoRiposting = true;
		boolean hurt;
		try {
			hurt = target.hurtServer(level, source, damage);
		} finally {
			autoRiposting = false;
		}
		if (hurt) {
			Vec3 toTarget = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
			Vec3 direction = toTarget.lengthSqr() < 1.0E-4 ? WeaponStats.horizontalLook(player) : toTarget.normalize();
			target.knockback(knockback, -direction.x, -direction.z);
			EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, spear);
			spear.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			player.setLastHurtMob(target);
		}
		Vec3 hit = target.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 8, 0.2, 0.2, 0.2, 0.12);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, hit.x, hit.y, hit.z, 6, 0.15, 0.2, 0.15, 0.05);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SPEAR_HIT, SoundSource.PLAYERS, 0.9F, 1.15F);
		return hurt;
	}

	private static boolean circularStrike(ServerPlayer player, LivingEntity focus, boolean enhanced, SpearPressure.Category category) {
		ServerLevel level = player.level();
		ItemStack spear = player.getMainHandItem();
		boolean boss = category == SpearPressure.Category.BOSS;
		enhanced &= !boss;
		float multiplier = boss ? SpearRules.CIRCULAR_BOSS_MULTIPLIER : enhanced ? SpearRules.CIRCULAR_ENHANCED_MULTIPLIER : SpearRules.CIRCULAR_MULTIPLIER;
		DamageSource source = level.damageSources().playerAttack(player);
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float primary = EnchantmentHelper.modifyDamage(level, spear, focus, source, base * multiplier);
		if (category == SpearPressure.Category.PVP) {
			primary = Math.min(primary, enhanced ? SpearRules.CIRCULAR_ENHANCED_PVP_CAP : SpearRules.CIRCULAR_PVP_CAP);
			primary = Math.min(primary, focus.getMaxHealth() * SpearRules.FINISHER_PVP_MAX_HEALTH_RATIO);
		}

		if (!hurtCircular(player, focus, spear, source, primary, SpearRules.CIRCULAR_KNOCKBACK)) {
			return false;
		}
		double radius = SpearRules.CIRCULAR_RADIUS;
		AABB area = player.getBoundingBox().inflate(radius);
		for (Entity entity : level.getEntities(player, area, candidate -> candidate instanceof LivingEntity living && living != focus && living.isAlive() && !living.isAlliedTo(player))) {
			LivingEntity living = (LivingEntity) entity;
			if (living.distanceToSqr(player) > radius * radius) {
				continue;
			}
			boolean secondaryPlayer = living instanceof Player;
			if (secondaryPlayer && living instanceof Player other && !player.canHarmPlayer(other)) {
				continue;
			}
			if (SpearPressure.isBoss(living)) {
				continue;
			}
			float ratio = secondaryPlayer ? SpearRules.CIRCULAR_SECONDARY_PLAYER_RATIO : SpearRules.CIRCULAR_SECONDARY_MOB_RATIO;
			float extra = EnchantmentHelper.modifyDamage(level, spear, living, source, base * multiplier * ratio);
			if (secondaryPlayer) {
				extra = Math.min(extra, SpearRules.CIRCULAR_SECONDARY_PVP_CAP);
			}
			hurtCircular(player, living, spear, source, extra, SpearRules.CIRCULAR_SECONDARY_KNOCKBACK);
		}

		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();
		if (!player.getAbilities().invulnerable) {
			player.causeFoodExhaustion(0.35F);
		}
		CIRCULAR_SPINS.put(player.getUUID(), SpearRules.CIRCULAR_SPIN_TICKS);
		ring(level, player, radius);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.65F);
		level.playSound(null, focus.getX(), focus.getY(), focus.getZ(), SoundEvents.SPEAR_HIT, SoundSource.PLAYERS, 1.0F, 0.7F);
		player.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.circular"), true);
		return true;
	}

	private static boolean hurtCircular(ServerPlayer player, LivingEntity target, ItemStack spear, DamageSource source, float damage, double knockback) {
		if (damage <= 0.0F || !target.isAlive()) {
			return false;
		}
		target.invulnerableTime = 0;
		autoRiposting = true;
		boolean hurt;
		try {
			hurt = target.hurtServer(player.level(), source, damage);
		} finally {
			autoRiposting = false;
		}
		if (!hurt) {
			return false;
		}
		Vec3 away = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
		Vec3 direction = away.lengthSqr() < 1.0E-4 ? WeaponStats.horizontalLook(player) : away.normalize();
		target.knockback(knockback, -direction.x, -direction.z);
		EnchantmentHelper.doPostAttackEffectsWithItemSource(player.level(), target, source, spear);
		spear.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		player.setLastHurtMob(target);
		return true;
	}

	private static void ring(ServerLevel level, ServerPlayer player, double radius) {
		Vec3 origin = player.position().add(0.0, 0.85, 0.0);
		for (int i = 0; i < 12; i++) {
			double angle = (Math.PI * 2.0 * i) / 12.0;
			double x = origin.x + Math.cos(angle) * radius * 0.55;
			double z = origin.z + Math.sin(angle) * radius * 0.55;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, origin.y, z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.CRIT, x, origin.y, z, 2, 0.08, 0.05, 0.08, 0.02);
		}
		level.sendParticles(ParticleTypes.CLOUD, origin.x, player.getY() + 0.1, origin.z, 14, radius * 0.35, 0.05, radius * 0.35, 0.02);
	}

	private static void tickSpins(MinecraftServer server) {
		if (CIRCULAR_SPINS.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Integer>> iterator = CIRCULAR_SPINS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			int left = entry.getValue() - 1;
			if (player == null || left < 0) {
				iterator.remove();
				continue;
			}
			player.setYRot(player.getYRot() + 60.0F);
			player.yRotO = player.getYRot();
			player.setYHeadRot(player.getYRot());
			player.hurtMarked = true;
			if (player.level() instanceof ServerLevel level && left % 2 == 0) {
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY(0.7), player.getZ(), 1, 0.4, 0.05, 0.4, 0.0);
			}
			entry.setValue(left);
			if (left == 0) {
				iterator.remove();
			}
		}
	}

	private static boolean strike(ServerPlayer player, LivingEntity target, boolean enhanced, SpearPressure.Category category) {
		ServerLevel level = player.level();
		ItemStack spear = player.getMainHandItem();
		boolean pvp = category == SpearPressure.Category.PVP;
		boolean boss = category == SpearPressure.Category.BOSS;
		enhanced &= !boss;
		float multiplier = boss ? SpearRules.FINISHER_BOSS_MULTIPLIER : enhanced ? SpearRules.FINISHER_ENHANCED_MULTIPLIER : SpearRules.FINISHER_MULTIPLIER;
		DamageSource source = level.damageSources().playerAttack(player);
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float damage = EnchantmentHelper.modifyDamage(level, spear, target, source, base * multiplier);
		if (pvp) {
			damage = Math.min(damage, enhanced ? SpearRules.FINISHER_ENHANCED_PVP_CAP : SpearRules.FINISHER_PVP_CAP);
			damage = Math.min(damage, target.getMaxHealth() * SpearRules.FINISHER_PVP_MAX_HEALTH_RATIO);
		}

		Vec3 toTarget = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
		Vec3 direction = toTarget.lengthSqr() < 1.0E-4 ? WeaponStats.horizontalLook(player) : toTarget.normalize();
		target.invulnerableTime = 0;
		autoRiposting = true;
		boolean hurt;
		try {
			hurt = target.hurtServer(level, source, damage);
		} finally {
			autoRiposting = false;
		}
		if (!hurt) {
			return false;
		}
		target.knockback(SpearRules.FINISHER_KNOCKBACK, -direction.x, -direction.z);
		EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, spear);
		spear.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		player.setLastHurtMob(target);
		player.swing(InteractionHand.MAIN_HAND, true);
		player.resetAttackStrengthTicker();
		if (!player.getAbilities().invulnerable) {
			player.causeFoodExhaustion(0.3F);
		}

		Vec3 eye = player.getEyePosition();
		Vec3 hit = target.getBoundingBox().getCenter();
		for (int i = 1; i <= 6; i++) {
			Vec3 point = eye.lerp(hit, i / 6.0);
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y - 0.2, point.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, hit.x, hit.y, hit.z, 12, 0.25, 0.3, 0.25, 0.1);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SPEAR_HIT, SoundSource.PLAYERS, 1.0F, 0.8F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9F, 0.7F);
		if (hurt && !pvp && !boss && !target.isAlive()) {
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.sendParticles(ParticleTypes.POOF, hit.x, hit.y, hit.z, 10, 0.3, 0.3, 0.3, 0.02);
		}
		if (enhanced) {
			wave(player, target);
		}
		return true;
	}

	private static void wave(ServerPlayer player, LivingEntity center) {
		ServerLevel level = player.level();
		double radius = SpearRules.ENHANCED_WAVE_RADIUS;
		AABB area = center.getBoundingBox().inflate(radius);
		for (Entity entity : level.getEntities(center, area, candidate -> candidate instanceof Mob mob && mob.isAlive() && !mob.isAlliedTo(player) && !SpearPressure.isBoss(mob))) {
			Mob mob = (Mob) entity;
			if (mob.distanceToSqr(center) > radius * radius) {
				continue;
			}
			Vec3 away = new Vec3(mob.getX() - center.getX(), 0.0, mob.getZ() - center.getZ());
			if (away.lengthSqr() > 1.0E-4) {
				away = away.normalize();
				mob.knockback(SpearRules.ENHANCED_WAVE_STRENGTH, -away.x, -away.z);
			}
			mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SpearRules.ENHANCED_WAVE_SLOW_TICKS, 0), player);
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.getX(), center.getY(0.4), center.getZ(), 3, radius * 0.4, 0.05, radius * 0.4, 0.0);
		level.sendParticles(ParticleTypes.CLOUD, center.getX(), center.getY(0.1), center.getZ(), 16, radius * 0.4, 0.05, radius * 0.4, 0.02);
		level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.7F);
	}

	static boolean inFinisherReach(ServerPlayer player, LivingEntity target) {
		if (!player.hasLineOfSight(target)) {
			return false;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		double reach = attackRange(player).effectiveMaxRange(player) + SpearRules.FINISHER_REACH_BONUS;
		AABB box = target.getBoundingBox().inflate(0.3);
		if (box.distanceToSqr(eye) > reach * reach) {
			return false;
		}
		if (box.contains(eye) || box.clip(eye, eye.add(look.scale(reach))).isPresent()) {
			return true;
		}
		Vec3 toCenter = box.getCenter().subtract(eye);
		return toCenter.lengthSqr() < 1.0E-6 || toCenter.normalize().dot(look) >= SpearRules.FINISHER_MIN_DOT;
	}

	private static boolean effectiveRange(ServerPlayer player, LivingEntity target) {
		AttackRange range = attackRange(player);
		double max = range.effectiveMaxRange(player);
		double distance = Math.sqrt(target.getBoundingBox().distanceToSqr(player.getEyePosition()));
		return distance >= max - SpearRules.JAB_EFFECTIVE_DEPTH;
	}

	private static AttackRange attackRange(LivingEntity entity) {
		AttackRange range = entity.getMainHandItem().get(DataComponents.ATTACK_RANGE);
		return range != null ? range : AttackRange.defaultFor(entity);
	}

	// Projéteis ficam de fora nesta versão: Contra Golpe só valida melee frontal com atacante.
	private static boolean isGuardableMelee(DamageSource source, LivingEntity attacker, ServerPlayer player) {
		if (attacker == player || source.getDirectEntity() != attacker || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_PROJECTILE)) {
			return false;
		}
		return source.is(DamageTypes.PLAYER_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
				|| source.is(DamageTypes.SPEAR)
				|| source.is(DamageTypes.STING)
				|| source.is(DamageTypes.MACE_SMASH);
	}

	private static boolean inFront(ServerPlayer player, LivingEntity attacker) {
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 toAttacker = new Vec3(attacker.getX() - player.getX(), 0.0, attacker.getZ() - player.getZ());
		if (toAttacker.lengthSqr() < 1.0E-4) {
			return true;
		}
		return toAttacker.normalize().dot(look) >= GUARD_MIN_DOT;
	}

	private static boolean canHoldGuard(ServerPlayer player) {
		return player.isAlive()
				&& !player.isSpectator()
				&& WeaponStats.isSpear(player.getMainHandItem())
				&& !ModEffects.isStunned(player)
				&& !ComboSpecials.isStunned(player)
				&& !TridentEffects.isStunned(player)
				&& !player.isInWater()
				&& !player.isInLava()
				&& !player.isPassenger()
				&& !player.isFallFlying()
				&& !player.getAbilities().flying
				&& !player.isUsingItem()
				&& !CombatantAccess.of(player).sliding();
	}

	private static void countPerfect(ServerPlayer player, LivingEntity attacker) {
		if (!(attacker instanceof Mob) || !(attacker instanceof Enemy) || unlocked(player)) {
			return;
		}
		int count = player.getAttachedOrElse(PERFECT_COUNTERS, 0) + 1;
		player.setAttached(PERFECT_COUNTERS, count);
		if (count >= SpearRules.PERFECT_COUNTER_GOAL) {
			grant(player);
		} else {
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.goal", Component.translatable("combat.modifiedpvp.combo.spear"), count, SpearRules.PERFECT_COUNTER_GOAL), true);
		}
	}

	private static boolean unlocked(ServerPlayer player) {
		AdvancementHolder holder = advancement(player);
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void grant(ServerPlayer player) {
		AdvancementHolder holder = advancement(player);
		if (holder != null) {
			player.getAdvancements().award(holder, "unlock");
		}
	}

	private static AdvancementHolder advancement(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		return server == null ? null : server.getAdvancements().get(ModifiedPvP.id(ADVANCEMENT));
	}

	private static int romperLevel(ServerPlayer player) {
		return ModEnchantments.level(player.level(), ModEnchantments.ROMPER, player.getMainHandItem());
	}

	private static void onGone(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		SpearPressure.forgetTarget(level.getServer(), entity.getUUID());
		SpearPressure.forget(entity.getUUID());
		GuardState state = STATES.get(entity.getUUID());
		if (state != null) {
			state.guardUntil = 0L;
			state.perfectUntil = 0L;
			state.riposteUntil = 0L;
			state.riposteTarget = null;
			state.followUpUntil = 0L;
			state.followUpTarget = null;
		}
		CIRCULAR_SPINS.remove(entity.getUUID());
		if (entity instanceof ServerPlayer player) {
			syncState(player);
		}
	}

	private static int remaining(long until, long now) {
		return (int) Math.max(0L, until - now);
	}

	private enum Kind {
		JAB,
		CHARGE
	}

	private enum Role {
		NONE,
		RIPOSTE,
		FOLLOW_UP
	}

	private static final class GuardState {
		private long guardUntil;
		private long perfectUntil;
		private boolean perfectUsed;
		private long perfectTick;
		private UUID perfectAttacker;
		private long cooldownUntil;
		private long riposteUntil;
		private UUID riposteTarget;
		private long followUpUntil;
		private UUID followUpTarget;
		private long finisherCooldownUntil;

		private boolean idle(long now) {
			return now >= this.guardUntil && now >= this.cooldownUntil && now >= this.riposteUntil
					&& now >= this.followUpUntil && now >= this.finisherCooldownUntil;
		}
	}

	private static final class JabContext {
		private final ServerPlayer player;
		private final float strength;
		private final Role role;
		private final UUID roleTarget;
		private final int romper;
		private final boolean guardCut;
		private boolean collapsed;
		private boolean roleHit;

		private JabContext(ServerPlayer player, float strength, Role role, UUID roleTarget, int romper, boolean guardCut) {
			this.player = player;
			this.strength = strength;
			this.role = role;
			this.roleTarget = roleTarget;
			this.romper = romper;
			this.guardCut = guardCut;
		}
	}

	private static final class StabFrame {
		private final ServerPlayer player;
		private final LivingEntity target;
		private final Kind kind;
		private final StabFrame previous;
		private boolean damaged;
		private boolean blocked;

		private StabFrame(ServerPlayer player, LivingEntity target, Kind kind, StabFrame previous) {
			this.player = player;
			this.target = target;
			this.kind = kind;
			this.previous = previous;
		}
	}
}
