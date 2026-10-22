package com.example.modifiedpvp.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ControlGuard {
	public enum Kind {
		STUN,
		INTERRUPT,
		PULL,
		KICK,
		KNOCK
	}

	private static final Map<UUID, Long> RESISTS = new HashMap<>();

	private ControlGuard() {
	}

	public static boolean tryApply(LivingEntity target, Kind kind) {
		if (!(target instanceof Player)) {
			return true;
		}
		Long until = RESISTS.get(target.getUUID());
		return until == null || target.level().getGameTime() >= until;
	}

	public static void mark(LivingEntity target, Kind kind) {
		if (!(target instanceof Player)) {
			return;
		}
		RESISTS.put(target.getUUID(), target.level().getGameTime() + CombatRules.CONTROL_RESIST_TICKS);
	}

	public static void stun(LivingEntity target, Entity source, int ticks) {
		if (ticks <= 0 || !tryApply(target, Kind.STUN)) {
			return;
		}
		target.addEffect(new MobEffectInstance(ModEffects.STUN, ticks, 0), source);
		mark(target, Kind.STUN);
	}

	public static void interrupt(LivingEntity target, Entity source, int ticks) {
		if (ticks <= 0 || !tryApply(target, Kind.INTERRUPT)) {
			return;
		}
		target.addEffect(new MobEffectInstance(ModEffects.INTERRUPTED, ticks, 0), source);
		mark(target, Kind.INTERRUPT);
	}

	public static void offBalance(LivingEntity target, Entity source, int ticks) {
		if (ticks <= 0 || !tryApply(target, Kind.INTERRUPT)) {
			return;
		}
		target.addEffect(new MobEffectInstance(ModEffects.OFF_BALANCE, ticks, 0), source);
		mark(target, Kind.INTERRUPT);
	}

	public static void exposeGuard(LivingEntity target, Entity source, int ticks) {
		if (ticks <= 0) {
			return;
		}
		target.addEffect(new MobEffectInstance(ModEffects.GUARD_EXPOSED, ticks, 0), source);
	}

	public static void tick(MinecraftServer server) {
		if (RESISTS.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, Long>> iterator = RESISTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();
			if (now >= entry.getValue() || server.getPlayerList().getPlayer(entry.getKey()) == null) {
				iterator.remove();
			}
		}
	}

	public static void forget(UUID id) {
		RESISTS.remove(id);
	}
}
