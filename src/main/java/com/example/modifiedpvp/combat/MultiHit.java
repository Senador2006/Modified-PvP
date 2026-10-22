package com.example.modifiedpvp.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class MultiHit {
	private static final Map<HitKey, Integer> SHOTGUN_HITS = new HashMap<>();
	private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);
	private static int nextVolley = 1;

	private MultiHit() {
	}

	public static int nextVolley() {
		int volley = nextVolley++;
		if (nextVolley >= Integer.MAX_VALUE / 2) {
			nextVolley = 1;
			SHOTGUN_HITS.clear();
		}
		return volley;
	}

	public static boolean applying() {
		return Boolean.TRUE.equals(APPLYING.get());
	}

	public static void beginApply() {
		APPLYING.set(true);
	}

	public static void endApply() {
		APPLYING.set(false);
	}

	public static float incoming(LivingEntity target, DamageSource source, float amount) {
		if (applying()) {
			return amount;
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof ShotgunAccess shotgun) {
			amount = scaleShotgun(shotgun.modifiedpvp$shotgunVolley(), target, amount);
		}
		if (direct instanceof ArrowVolleyAccess volley && volley.modifiedpvp$isVolleyCompanion()) {
			amount = scaleCompanion(target, amount);
		}
		return amount;
	}

	public static float scaleShotgun(int volley, LivingEntity target, float amount) {
		if (volley <= 0 || amount <= 0.0F || !(target instanceof Player)) {
			return amount;
		}
		int hits = SHOTGUN_HITS.merge(new HitKey(volley, target.getUUID()), 1, Integer::sum);
		if (hits <= 1) {
			return amount;
		}
		return amount * (float) Math.pow(CombatRules.SHOTGUN_PLAYER_FALLOFF, hits - 1);
	}

	public static float scaleCompanion(LivingEntity target, float amount) {
		if (amount <= 0.0F || !(target instanceof Player)) {
			return amount;
		}
		return amount * CombatRules.DOUBLE_ARROW_PVP_RATIO;
	}

	public static void clear() {
		SHOTGUN_HITS.clear();
	}

	private record HitKey(int volley, UUID target) {
	}
}
