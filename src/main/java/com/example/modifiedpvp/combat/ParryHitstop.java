package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.modifiedpvp.network.ModNetworking;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class ParryHitstop {
	private static final List<Pending> PENDING = new ArrayList<>();
	private static volatile Set<UUID> HELD = Set.of();

	private ParryHitstop() {
	}

	public static boolean holds(Entity entity) {
		return entity != null && HELD.contains(entity.getUUID());
	}

	public static void armReflect(ServerPlayer defender, LivingEntity attacker, float reflectedDamage) {
		arm(defender, attacker, Kind.REFLECT, reflectedDamage);
	}

	public static void armSpear(ServerPlayer defender, LivingEntity attacker) {
		arm(defender, attacker, Kind.SPEAR, 0.0F);
	}

	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Pending hit : new ArrayList<>(PENDING)) {
			if (!PENDING.contains(hit)) {
				continue;
			}
			LivingEntity defender = find(server, hit.defender);
			LivingEntity attacker = find(server, hit.attacker);
			if (!(defender instanceof ServerPlayer player) || attacker == null || !player.isAlive() || !attacker.isAlive()) {
				releaseMob(attacker, hit);
				PENDING.remove(hit);
				publish();
				continue;
			}
			if (--hit.ticks > 0) {
				pin(player, hit.defenderAnchor);
				pin(attacker, hit.attackerAnchor);
				continue;
			}
			releaseMob(attacker, hit);
			PENDING.remove(hit);
			publish();
			if (hit.kind == Kind.SPEAR) {
				SpearCombat.resolvePerfectRiposte(player, attacker);
			} else {
				CombatActions.finishParry(player, attacker, hit.damage);
			}
		}
	}

	private static void arm(ServerPlayer defender, LivingEntity attacker, Kind kind, float damage) {
		if (holds(defender)) {
			return;
		}
		Pending hit = new Pending(defender.getUUID(), attacker.getUUID(), kind, damage, CombatRules.PARRY_HITSTOP_TICKS, Anchor.capture(defender), Anchor.capture(attacker));
		if (attacker instanceof Mob mob) {
			hit.attackerWasNoAi = mob.isNoAi();
			mob.setNoAi(true);
			hit.frozeAttackerAi = true;
		}
		PENDING.add(hit);
		publish();
		pin(defender, hit.defenderAnchor);
		pin(attacker, hit.attackerAnchor);

		ServerLevel level = defender.level();
		level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), ModSounds.PARRY, SoundSource.PLAYERS, 1.0F, 0.9F + defender.getRandom().nextFloat() * 0.15F);
		Vec3 spark = defender.getEyePosition().add(WeaponStats.horizontalLook(defender).scale(0.55));
		level.sendParticles(ParticleTypes.CRIT, spark.x, spark.y - 0.25, spark.z, 14, 0.04, 0.04, 0.04, 0.02);
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, spark.x, spark.y - 0.25, spark.z, 8, 0.05, 0.05, 0.05, 0.0);
		ModNetworking.syncParryImpact(defender, CombatRules.PARRY_HITSTOP_TICKS);
		if (attacker instanceof ServerPlayer other) {
			ModNetworking.syncParryImpact(other, CombatRules.PARRY_HITSTOP_TICKS);
		}
	}

	private static void pin(Entity entity, Anchor anchor) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.hurtMarked = true;
		entity.setPos(anchor.x(), anchor.y(), anchor.z());
		entity.setYRot(anchor.yRot());
		entity.setXRot(anchor.xRot());
		if (entity instanceof LivingEntity living) {
			living.setYHeadRot(anchor.yRot());
			living.yBodyRot = anchor.yRot();
			living.yBodyRotO = anchor.yRot();
			living.yHeadRotO = anchor.yRot();
		}
	}

	private static void releaseMob(LivingEntity attacker, Pending hit) {
		if (hit.frozeAttackerAi && attacker instanceof Mob mob) {
			mob.setNoAi(hit.attackerWasNoAi);
			hit.frozeAttackerAi = false;
		}
	}

	private static LivingEntity find(MinecraftServer server, UUID id) {
		ServerPlayer player = server.getPlayerList().getPlayer(id);
		if (player != null) {
			return player;
		}
		Entity entity = server.overworld().getEntityInAnyDimension(id);
		return entity instanceof LivingEntity living ? living : null;
	}

	private static void publish() {
		if (PENDING.isEmpty()) {
			HELD = Set.of();
			return;
		}
		Set<UUID> ids = new HashSet<>();
		for (Pending hit : PENDING) {
			ids.add(hit.defender);
			ids.add(hit.attacker);
		}
		HELD = Set.copyOf(ids);
	}

	private enum Kind {
		REFLECT,
		SPEAR
	}

	private static final class Pending {
		private final UUID defender;
		private final UUID attacker;
		private final Kind kind;
		private final float damage;
		private int ticks;
		private final Anchor defenderAnchor;
		private final Anchor attackerAnchor;
		private boolean frozeAttackerAi;
		private boolean attackerWasNoAi;

		private Pending(UUID defender, UUID attacker, Kind kind, float damage, int ticks, Anchor defenderAnchor, Anchor attackerAnchor) {
			this.defender = defender;
			this.attacker = attacker;
			this.kind = kind;
			this.damage = damage;
			this.ticks = ticks;
			this.defenderAnchor = defenderAnchor;
			this.attackerAnchor = attackerAnchor;
		}
	}

	private record Anchor(double x, double y, double z, float yRot, float xRot) {
		private static Anchor capture(Entity entity) {
			return new Anchor(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
		}
	}
}
