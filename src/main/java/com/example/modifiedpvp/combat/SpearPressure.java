package com.example.modifiedpvp.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.network.ModNetworking;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;

public final class SpearPressure {
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, ModifiedPvP.id("spear_bosses"));

	private static final Map<PairKey, Entry> PRESSURE = new HashMap<>();
	private static final Map<UUID, Opening> OPENINGS = new HashMap<>();

	private SpearPressure() {
	}

	static void add(ServerPlayer attacker, LivingEntity target, float points, Cause cause, int romper) {
		if (points <= 0.0F || !eligible(attacker, target)) {
			return;
		}
		long now = attacker.level().getGameTime();
		PairKey key = new PairKey(attacker.getUUID(), target.getUUID());
		Entry entry = PRESSURE.computeIfAbsent(key, ignored -> new Entry(target));
		entry.target = target;
		if (cause == Cause.CHARGE) {
			if (now - entry.lastCharge < SpearRules.CHARGE_REPEAT_TICKS) {
				return;
			}
			entry.lastCharge = now;
		}
		if (cause == Cause.SHIELD) {
			if (now - entry.lastShield < SpearRules.CHARGE_REPEAT_TICKS) {
				return;
			}
			entry.lastShield = now;
		}
		entry.lastGain = now;
		switch (cause) {
			case RIPOSTE, FOLLOW_UP -> entry.counter += points;
			case CHARGE -> entry.kinetic += points;
			default -> entry.formation += points;
		}
		entry.value = Math.min(SpearRules.PRESSURE_MAX, entry.value + points);
		if (entry.value >= SpearRules.PRESSURE_MAX) {
			Opening current = OPENINGS.get(target.getUUID());
			if (current != null && now < current.immunityUntil) {
				entry.value = SpearRules.PRESSURE_MAX - 1.0F;
			} else {
				entry.value = 0.0F;
				boolean riposte = cause == Cause.RIPOSTE || cause == Cause.FOLLOW_UP;
				breakDefense(attacker, target, riposte && romper >= 3, now);
			}
		}
		send(attacker, target, entry.value, now);
	}

	static Opening openingOn(ServerPlayer player, LivingEntity target, long now) {
		Opening opening = OPENINGS.get(target.getUUID());
		if (opening == null || opening.consumed || now >= opening.until || !opening.owner.equals(player.getUUID())) {
			return null;
		}
		if (!target.isAlive() || target.isRemoved() || target.level() != player.level()) {
			return null;
		}
		return opening;
	}

	static Opening openingAimed(ServerPlayer player, long now) {
		Opening best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Opening opening : OPENINGS.values()) {
			if (!opening.owner.equals(player.getUUID()) || opening.consumed || now >= opening.until) {
				continue;
			}
			LivingEntity target = opening.target;
			if (!target.isAlive() || target.isRemoved() || target.level() != player.level()) {
				continue;
			}
			if (!SpearCombat.inFinisherReach(player, target)) {
				continue;
			}
			double distance = player.distanceToSqr(target);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = opening;
			}
		}
		return best;
	}

	static void consume(ServerPlayer owner, Opening opening, long now) {
		opening.consumed = true;
		opening.until = now;
		send(owner, opening.target, currentValue(owner, opening.target), now);
		if (opening.target instanceof ServerPlayer victim) {
			SpearCombat.syncState(victim);
		}
	}

	static int brokenTicksOn(LivingEntity target, long now) {
		Opening opening = OPENINGS.get(target.getUUID());
		if (opening == null || opening.consumed) {
			return 0;
		}
		return (int) Math.max(0L, opening.until - now);
	}

	public static void tick(MinecraftServer server) {
		if (PRESSURE.isEmpty() && OPENINGS.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		float decay = SpearRules.PRESSURE_DECAY_PER_SECOND / 20.0F;
		Iterator<Map.Entry<PairKey, Entry>> pressure = PRESSURE.entrySet().iterator();
		while (pressure.hasNext()) {
			Map.Entry<PairKey, Entry> mapped = pressure.next();
			Entry entry = mapped.getValue();
			ServerPlayer attacker = server.getPlayerList().getPlayer(mapped.getKey().attacker());
			LivingEntity target = entry.target;
			if (attacker == null || !target.isAlive() || target.isRemoved() || attacker.level() != target.level()) {
				pressure.remove();
				if (attacker != null) {
					sendRaw(attacker, target, 0.0F, now);
				}
				continue;
			}
			if (now - entry.lastGain <= SpearRules.PRESSURE_DECAY_DELAY_TICKS) {
				continue;
			}
			entry.value -= decay;
			if (entry.value <= 0.0F) {
				pressure.remove();
				sendRaw(attacker, target, 0.0F, now);
			} else if (Math.round(entry.value) != entry.sent) {
				entry.sent = Math.round(entry.value);
				sendRaw(attacker, target, entry.value, now);
			}
		}

		Iterator<Opening> openings = OPENINGS.values().iterator();
		while (openings.hasNext()) {
			Opening opening = openings.next();
			LivingEntity target = opening.target;
			if (!target.isAlive() || target.isRemoved()) {
				openings.remove();
				notifyOwner(server, opening, now);
				continue;
			}
			if (now < opening.until) {
				holdOpening(opening, now);
			} else if (!opening.closed) {
				opening.closed = true;
				notifyOwner(server, opening, now);
				if (target instanceof ServerPlayer victim) {
					SpearCombat.syncState(victim);
				}
			}
			if (now >= opening.immunityUntil) {
				openings.remove();
				notifyOwner(server, opening, now);
			}
		}
	}

	public static void forget(UUID id) {
		Iterator<Map.Entry<PairKey, Entry>> pressure = PRESSURE.entrySet().iterator();
		while (pressure.hasNext()) {
			Map.Entry<PairKey, Entry> mapped = pressure.next();
			PairKey key = mapped.getKey();
			if (key.attacker().equals(id) || key.target().equals(id)) {
				pressure.remove();
			}
		}
		OPENINGS.entrySet().removeIf(entry -> entry.getKey().equals(id) || entry.getValue().owner.equals(id));
	}

	public static void forgetTarget(MinecraftServer server, UUID target) {
		Iterator<Map.Entry<PairKey, Entry>> pressure = PRESSURE.entrySet().iterator();
		while (pressure.hasNext()) {
			Map.Entry<PairKey, Entry> mapped = pressure.next();
			if (!mapped.getKey().target().equals(target)) {
				continue;
			}
			pressure.remove();
			ServerPlayer attacker = server.getPlayerList().getPlayer(mapped.getKey().attacker());
			if (attacker != null) {
				sendRaw(attacker, mapped.getValue().target, 0.0F, server.overworld().getGameTime());
			}
		}
		OPENINGS.remove(target);
	}

	public static void clear() {
		PRESSURE.clear();
		OPENINGS.clear();
	}

	static boolean isBoss(LivingEntity entity) {
		return entity.getType().is(BOSSES);
	}

	private static void breakDefense(ServerPlayer attacker, LivingEntity target, boolean enhanced, long now) {
		Category category = target instanceof Player ? Category.PVP : isBoss(target) ? Category.BOSS : Category.COMMON;
		int duration = switch (category) {
			case PVP -> SpearRules.BREAK_PVP_TICKS;
			case BOSS -> SpearRules.BREAK_BOSS_TICKS;
			case COMMON -> SpearRules.BREAK_MOB_TICKS;
		};
		int immunity = switch (category) {
			case PVP -> SpearRules.BREAK_PVP_IMMUNITY_TICKS;
			case BOSS -> SpearRules.BREAK_BOSS_IMMUNITY_TICKS;
			case COMMON -> SpearRules.BREAK_MOB_IMMUNITY_TICKS;
		};
		Opening opening = new Opening(target, attacker.getUUID(), now + duration, now + immunity, enhanced && category != Category.BOSS, category, dominant(PRESSURE.get(new PairKey(attacker.getUUID(), target.getUUID()))));
		OPENINGS.put(target.getUUID(), opening);
		attacker.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.broken"), true);
		if (target instanceof ServerPlayer victim) {
			victim.displayClientMessage(Component.translatable("combat.modifiedpvp.spear.broken_self"), true);
		}

		ServerLevel level = attacker.level();
		if (category != Category.BOSS && target.isBlocking()) {
			ItemStack blocking = target.getUseItem();
			BlocksAttacks blocks = blocking.get(DataComponents.BLOCKS_ATTACKS);
			if (blocks != null) {
				blocks.disable(level, target, duration / 20.0F, blocking);
			}
		}
		if (category == Category.PVP) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, SpearRules.BREAK_PVP_SLOW_AMPLIFIER), attacker);
		} else if (category == Category.COMMON && target instanceof Mob mob) {
			mob.getNavigation().stop();
		}

		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.7F, 1.25F);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6), target.getZ(), 14, 0.3, 0.35, 0.3, 0.05);
		if (target instanceof ServerPlayer victim) {
			SpearCombat.syncState(victim);
		}
	}

	private static void holdOpening(Opening opening, long now) {
		LivingEntity target = opening.target;
		if (opening.category == Category.COMMON && target instanceof Mob mob) {
			mob.getNavigation().stop();
			target.setDeltaMovement(target.getDeltaMovement().multiply(0.3, 1.0, 0.3));
			target.hurtMarked = true;
		}
		if (now % 5L == 0L && target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(1.05), target.getZ(), 2, 0.2, 0.05, 0.2, 0.0);
		}
	}

	private static void notifyOwner(MinecraftServer server, Opening opening, long now) {
		ServerPlayer owner = server.getPlayerList().getPlayer(opening.owner);
		if (owner != null) {
			send(owner, opening.target, currentValue(owner, opening.target), now);
		}
	}

	private static float currentValue(ServerPlayer attacker, LivingEntity target) {
		Entry entry = PRESSURE.get(new PairKey(attacker.getUUID(), target.getUUID()));
		return entry == null ? 0.0F : entry.value;
	}

	private static void send(ServerPlayer attacker, LivingEntity target, float value, long now) {
		Entry entry = PRESSURE.get(new PairKey(attacker.getUUID(), target.getUUID()));
		if (entry != null) {
			entry.sent = Math.round(value);
		}
		sendRaw(attacker, target, value, now);
	}

	private static void sendRaw(ServerPlayer attacker, LivingEntity target, float value, long now) {
		Opening opening = OPENINGS.get(target.getUUID());
		int broken = 0;
		boolean enhanced = false;
		boolean immune = false;
		if (opening != null) {
			immune = now < opening.immunityUntil;
			if (opening.owner.equals(attacker.getUUID()) && !opening.consumed && now < opening.until) {
				broken = (int) (opening.until - now);
				enhanced = opening.enhanced;
			}
		}
		ModNetworking.syncSpearPressure(attacker, target.getId(), Math.round(value), broken, enhanced, immune, styleHint(opening, attacker));
	}

	private static int styleHint(Opening opening, ServerPlayer attacker) {
		if (opening != null && opening.owner.equals(attacker.getUUID()) && !opening.consumed) {
			return opening.style.ordinal();
		}
		return Style.NONE.ordinal();
	}

	private static Style dominant(Entry entry) {
		if (entry == null) {
			return Style.FORMATION;
		}
		if (entry.counter >= entry.kinetic && entry.counter >= entry.formation) {
			return Style.COUNTER;
		}
		if (entry.kinetic >= entry.formation) {
			return Style.KINETIC;
		}
		return Style.FORMATION;
	}

	private static boolean eligible(ServerPlayer attacker, LivingEntity target) {
		if (target == attacker || !target.isAlive() || target.isRemoved() || target.level() != attacker.level()) {
			return false;
		}
		if (target instanceof Player other) {
			return !other.isSpectator() && !other.getAbilities().invulnerable && attacker.canHarmPlayer(other);
		}
		return target instanceof Mob;
	}

	enum Cause {
		JAB,
		CHARGE,
		SHIELD,
		RIPOSTE,
		FOLLOW_UP
	}

	enum Category {
		PVP,
		COMMON,
		BOSS
	}

	enum Style {
		NONE,
		COUNTER,
		KINETIC,
		FORMATION
	}

	private record PairKey(UUID attacker, UUID target) {
	}

	private static final class Entry {
		private LivingEntity target;
		private float value;
		private float counter;
		private float kinetic;
		private float formation;
		private long lastGain;
		private long lastCharge = Long.MIN_VALUE / 2;
		private long lastShield = Long.MIN_VALUE / 2;
		private int sent = -1;

		private Entry(LivingEntity target) {
			this.target = target;
		}
	}

	static final class Opening {
		final LivingEntity target;
		final UUID owner;
		final long immunityUntil;
		final boolean enhanced;
		final Category category;
		final Style style;
		long until;
		boolean consumed;
		boolean closed;

		private Opening(LivingEntity target, UUID owner, long until, long immunityUntil, boolean enhanced, Category category, Style style) {
			this.target = target;
			this.owner = owner;
			this.until = until;
			this.immunityUntil = immunityUntil;
			this.enhanced = enhanced;
			this.category = category;
			this.style = style == null ? Style.FORMATION : style;
		}
	}
}
