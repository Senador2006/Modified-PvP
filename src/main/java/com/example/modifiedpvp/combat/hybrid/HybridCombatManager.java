package com.example.modifiedpvp.combat.hybrid;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.ComboSpecials;
import com.example.modifiedpvp.combat.WeaponStats;
import com.example.modifiedpvp.combat.adaptive.WeaponFamily;
import com.example.modifiedpvp.network.HybridStatePayload;
import com.example.modifiedpvp.network.ModNetworking;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class HybridCombatManager {
	public static final ResourceKey<Enchantment> LUNGE = ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("lunge"));
	public static final ResourceKey<Enchantment> WIND_BURST = ResourceKey.create(Registries.ENCHANTMENT, Identifier.withDefaultNamespace("wind_burst"));
	private static final Map<UUID, Runtime> STATES = new HashMap<>();
	private static final Map<UUID, UUID> SHOTS = new HashMap<>();
	private static HybridBalanceConfig config = HybridBalanceConfig.defaults();
	private static boolean reachWarned;

	private HybridCombatManager() {
	}

	public static void configure(HybridBalanceConfig loaded) {
		config = loaded == null ? HybridBalanceConfig.defaults() : loaded;
		if (config.reachEnabled() && !reachWarned) {
			reachWarned = true;
			ModifiedPvP.LOGGER.info("spearMaceExtendedReachEnabled is observational in 1.3.1 and does not change mace reach");
		}
	}

	public static HybridBalanceConfig config() {
		return config;
	}

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Runtime runtime = STATES.get(player.getUUID());
			if (runtime == null) {
				continue;
			}
			trackAir(player, runtime, now);
			trackSpear(player, runtime, now);
			if (runtime.engine.tick(now)) {
				sync(player, runtime, now);
			}
		}
	}

	public static void clear(ServerPlayer player) {
		Runtime runtime = STATES.remove(player.getUUID());
		if (runtime != null) {
			runtime.engine.clear();
			sync(player, runtime, player.level().getGameTime());
		}
	}

	public static void bindShot(UUID projectileId, UUID shotId) {
		if (projectileId != null && shotId != null) {
			SHOTS.put(projectileId, shotId);
			if (SHOTS.size() > 512) {
				var iterator = SHOTS.keySet().iterator();
				if (iterator.hasNext()) {
					iterator.next();
					iterator.remove();
				}
			}
		}
	}

	public static boolean claims(Player player, HybridAction action) {
		if (!config.enabled() || !(player instanceof ServerPlayer)) {
			return false;
		}
		Runtime runtime = STATES.get(player.getUUID());
		return runtime != null && runtime.engine.claims(action);
	}

	public static int heavyFavor(ServerPlayer player) {
		Runtime runtime = STATES.get(player.getUUID());
		if (runtime == null || player.level().getGameTime() > runtime.chargeFavorUntil) {
			return 0;
		}
		return config.chargeFavor();
	}

	public static void onHeavyStart(ServerPlayer player) {
		Runtime runtime = STATES.get(player.getUUID());
		if (runtime != null) {
			runtime.frontalUntil = 0L;
		}
	}

	public static float mitigate(ServerPlayer player, DamageSource source, float amount) {
		Runtime runtime = STATES.get(player.getUUID());
		if (runtime == null || player.level().getGameTime() > runtime.frontalUntil || source.getEntity() == null) {
			return amount;
		}
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 toward = source.getEntity().position().subtract(player.position());
		double flat = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
		if (flat < 1.0E-4) {
			return amount;
		}
		double dot = (toward.x * look.x + toward.z * look.z) / flat;
		return dot > 0.35 ? amount * 0.65F : amount;
	}

	public static void onWind(ServerPlayer player, boolean wind) {
		Runtime runtime = state(player);
		if (runtime.air.grounded) {
			runtime.air.pendingAerial = true;
			runtime.air.pendingWind |= wind;
		} else {
			runtime.air.aerial = true;
			runtime.air.wind |= wind;
		}
	}

	public static void onSpearLunge(ServerPlayer player) {
		Runtime runtime = state(player);
		trackAir(player, runtime, player.level().getGameTime());
		if (!player.onGround() && !player.isInWater() && !player.isFallFlying() && !player.isPassenger()) {
			runtime.air.lunge = true;
			runtime.air.spear = true;
		}
	}

	public static void onSpearHit(ServerPlayer player, LivingEntity target, boolean hit) {
		Runtime runtime = state(player);
		runtime.air.spear = true;
		offer(player, fact(player, HybridAction.SPEAR_HIT, target).hit(hit), target);
	}

	public static HybridResult onSwordHit(ServerPlayer player, LivingEntity target, boolean hit, boolean blocked) {
		if (WeaponStats.isDualWielding(player) || !WeaponStats.isSword(player.getMainHandItem())) {
			return HybridResult.IGNORED;
		}
		return offer(player, fact(player, HybridAction.SWORD_HIT, target).hit(hit).blocked(blocked).fullCharge(charged(player)).tridentAway(away(player)), target);
	}

	public static HybridResult onThrust(ServerPlayer player, LivingEntity target, boolean hit) {
		if (WeaponStats.isDualWielding(player) || !WeaponStats.isSword(player.getMainHandItem())) {
			return HybridResult.IGNORED;
		}
		return offer(player, fact(player, HybridAction.SWORD_THRUST, target).hit(hit).fullCharge(charged(player)).tridentAway(away(player)), target);
	}

	public static HybridResult onHeavy(ServerPlayer player, LivingEntity target, boolean hit, boolean blocked, boolean fullCharge) {
		return offer(player, fact(player, HybridAction.AXE_HEAVY, target).hit(hit).blocked(blocked).fullCharge(fullCharge), target);
	}

	public static void onShield(ServerPlayer player, LivingEntity attacker, boolean timed) {
		Vec3 look = WeaponStats.horizontalLook(player);
		Vec3 motion = player.getDeltaMovement();
		boolean advancing = motion.x * look.x + motion.z * look.z > 0.04;
		offer(player, fact(player, HybridAction.SHIELD_BLOCK, attacker).hit(true).blocked(true).timed(timed).advancing(advancing), attacker);
	}

	public static void onTridentLoosed(ServerPlayer player) {
		Runtime runtime = state(player);
		runtime.away++;
		offer(player, fact(player, HybridAction.TRIDENT_THROW, null).hit(true), null);
	}

	public static void onTridentHit(ServerPlayer player, LivingEntity target, UUID projectileId, boolean loyalty) {
		Distance distance = distance(player, target);
		offer(player, fact(player, HybridAction.TRIDENT_HIT, target)
				.projectile(projectileId)
				.loyalty(loyalty)
				.distanceIncreasing(distance.increasing())
				.airborne(!player.onGround()), target);
	}

	public static void onTridentReturn(ServerPlayer player, UUID projectileId) {
		Runtime runtime = state(player);
		runtime.away = Math.max(0, runtime.away - 1);
		offer(player, fact(player, HybridAction.TRIDENT_RETURN, null).projectile(projectileId).loyalty(true).hit(true), null);
	}

	public static void onPull(ServerPlayer player, LivingEntity target) {
		offer(player, fact(player, HybridAction.PULL, target).pull(true).loyalty(true).hit(true), target);
	}

	public static HybridResult onMace(ServerPlayer player, LivingEntity target, boolean smash, boolean windBurst) {
		HybridFact.Builder builder = fact(player, smash ? HybridAction.MACE_SMASH : HybridAction.MACE_HIT, target);
		Runtime runtime = state(player);
		if (smash) {
			builder.airborne(true).validFall(true)
					.lateSwap(runtime.air.lateSwap)
					.sequenceWind(runtime.air.wind)
					.sequenceLunge(runtime.air.lunge)
					.sequenceSpear(runtime.air.spear)
					.sequenceAerial(runtime.air.aerial)
					.aerial(runtime.air.id);
		}
		HybridResult result = offer(player, builder, target);
		if (smash && windBurst) {
			runtime.air.wind = true;
			runtime.air.aerial = true;
		}
		return result;
	}

	public static HybridResult onCrossbowArrow(ServerPlayer player, LivingEntity target, UUID projectileId) {
		UUID shot = SHOTS.remove(projectileId);
		if (shot == null) {
			return HybridResult.IGNORED;
		}
		Distance distance = distance(player, target);
		return offer(player, fact(player, HybridAction.CROSSBOW_ARROW, target).actionId(shot).approaching(distance.approaching()), target);
	}

	public static HybridResult onCrossbowFirework(ServerPlayer player, LivingEntity focus, int affected, UUID projectileId) {
		UUID shot = projectileId == null ? null : SHOTS.remove(projectileId);
		if (shot == null) {
			return HybridResult.IGNORED;
		}
		return offer(player, fact(player, HybridAction.CROSSBOW_FIREWORK, focus).actionId(shot).hit(affected > 0).affected(affected).approaching(focus != null && distance(player, focus).approaching()), focus);
	}

	public static boolean tryAdvance(ServerPlayer player) {
		if (!config.enabled() || !WeaponStats.canUseMovement(player)) {
			return false;
		}
		if (!WeaponStats.isAxe(player.getMainHandItem()) || !WeaponStats.isShield(player.getOffhandItem())) {
			return false;
		}
		Runtime runtime = state(player);
		if (runtime.engine.style() != HybridStyleId.BASTION_ADVANCE) {
			return false;
		}
		HybridResult result = offer(player, fact(player, HybridAction.GUARD_ADVANCE, null).hit(true), null);
		if (result.verdict() == HybridVerdict.EXECUTED) {
			runtime.frontalUntil = player.level().getGameTime() + config.guardTicks();
			return true;
		}
		return false;
	}

	private static HybridResult offer(ServerPlayer player, HybridFact.Builder builder, LivingEntity target) {
		if (!config.enabled()) {
			return HybridResult.IGNORED;
		}
		Runtime runtime = state(player);
		long now = player.level().getGameTime();
		trackAir(player, runtime, now);
		HybridFact fact = builder.build();
		HybridResult result = runtime.engine.offer(fact, equipped(player));
		if (result.verdict() == HybridVerdict.PRIMED && result.style() == HybridStyleId.SOVEREIGN_RETALIATION) {
			runtime.chargeFavorUntil = now + config.balance().primeWindow();
		}
		if (result.verdict() == HybridVerdict.EXECUTED && result.style() != null) {
			HybridRewards.apply(player, target, result.style(), fact, config);
			if (result.style().crown() && fact.hostile()) {
				ComboSpecials.rememberCrown(player, result.style());
			}
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(result.style().readyKey()), true);
		} else if (result.verdict() == HybridVerdict.PREPARED && result.style() != null) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable(result.style().readyKey()), true);
		} else if (result.verdict() == HybridVerdict.FAILED) {
			player.displayClientMessage(net.minecraft.network.chat.Component.translatable("combat.modifiedpvp.hybrid.miss"), true);
		}
		if (config.debug() && result.verdict() != HybridVerdict.IGNORED && result.verdict() != HybridVerdict.OBSERVED) {
			ModifiedPvP.LOGGER.info("hybrid {} {} for {}", result.verdict(), result.style(), player.getGameProfile().name());
		}
		sync(player, runtime, now);
		return result;
	}

	private static void trackAir(ServerPlayer player, Runtime runtime, long now) {
		boolean grounded = !player.isAlive() || player.onGround() || player.isInWater() || player.isFallFlying();
		Air air = runtime.air;
		if (grounded) {
			if (!air.grounded) {
				air.wind = false;
				air.lunge = false;
				air.spear = false;
				air.aerial = false;
				air.lateSwap = false;
			}
			air.grounded = true;
			return;
		}
		if (air.grounded) {
			air.id++;
			air.wind = air.pendingWind;
			air.aerial = air.pendingAerial || air.pendingWind;
			air.pendingWind = false;
			air.pendingAerial = false;
			air.lunge = false;
			air.spear = false;
			air.lateSwap = false;
		}
		air.grounded = false;
		if (air.spearTick >= 0 && now - air.spearTick > config.swapWindow()) {
			air.lateSwap = false;
		}
	}

	private static void trackSpear(ServerPlayer player, Runtime runtime, long now) {
		ItemStack main = player.getMainHandItem();
		if (WeaponStats.isSpear(main)) {
			runtime.air.spearTick = now;
		}
		if (main.is(Items.MACE) && runtime.air.spearTick >= 0 && now - runtime.air.spearTick <= config.swapWindow()) {
			runtime.air.lateSwap = true;
		}
	}

	private static EnumSet<HybridPair> equipped(ServerPlayer player) {
		EnumSet<HybridPair> pairs = EnumSet.noneOf(HybridPair.class);
		boolean sword = false;
		boolean axe = false;
		boolean mace = false;
		boolean spear = false;
		boolean trident = false;
		boolean crossbow = false;
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			sword |= WeaponStats.isSword(stack);
			axe |= WeaponStats.isAxe(stack);
			mace |= stack.is(Items.MACE);
			spear |= WeaponStats.isSpear(stack);
			trident |= stack.is(Items.TRIDENT);
			crossbow |= stack.is(Items.CROSSBOW);
		}
		ItemStack offhand = player.getOffhandItem();
		sword |= WeaponStats.isSword(offhand);
		axe |= WeaponStats.isAxe(offhand);
		mace |= offhand.is(Items.MACE);
		spear |= WeaponStats.isSpear(offhand);
		trident |= offhand.is(Items.TRIDENT);
		crossbow |= offhand.is(Items.CROSSBOW);
		boolean shield = WeaponStats.isShield(offhand);
		if (spear && mace && allowed(player, HybridPair.SPEAR_MACE)) {
			pairs.add(HybridPair.SPEAR_MACE);
		}
		if (sword && trident && allowed(player, HybridPair.SWORD_TRIDENT)) {
			pairs.add(HybridPair.SWORD_TRIDENT);
		}
		if (mace && trident && allowed(player, HybridPair.MACE_TRIDENT)) {
			pairs.add(HybridPair.MACE_TRIDENT);
		}
		if (axe && shield && allowed(player, HybridPair.AXE_SHIELD)) {
			pairs.add(HybridPair.AXE_SHIELD);
		}
		if (sword && shield && allowed(player, HybridPair.SWORD_SHIELD)) {
			pairs.add(HybridPair.SWORD_SHIELD);
		}
		if (sword && crossbow && allowed(player, HybridPair.SWORD_CROSSBOW)) {
			pairs.add(HybridPair.SWORD_CROSSBOW);
		}
		if (axe && crossbow && allowed(player, HybridPair.AXE_CROSSBOW)) {
			pairs.add(HybridPair.AXE_CROSSBOW);
		}
		return pairs;
	}

	private static boolean allowed(ServerPlayer player, HybridPair pair) {
		if (!config.requireAdvancements()) {
			return true;
		}
		return switch (pair) {
			case SPEAR_MACE -> ComboSpecials.unlockedFamily(player, WeaponFamily.SPEAR) && ComboSpecials.unlockedFamily(player, WeaponFamily.MACE);
			case SWORD_TRIDENT -> ComboSpecials.unlockedFamily(player, WeaponFamily.SWORD) && ComboSpecials.unlockedFamily(player, WeaponFamily.TRIDENT);
			case MACE_TRIDENT -> ComboSpecials.unlockedFamily(player, WeaponFamily.MACE) && ComboSpecials.unlockedFamily(player, WeaponFamily.TRIDENT);
			case AXE_SHIELD -> ComboSpecials.unlockedFamily(player, WeaponFamily.AXE) && ComboSpecials.unlockedFamily(player, WeaponFamily.SHIELD);
			case SWORD_SHIELD -> ComboSpecials.unlockedFamily(player, WeaponFamily.SWORD) && ComboSpecials.unlockedFamily(player, WeaponFamily.SHIELD);
			case SWORD_CROSSBOW -> ComboSpecials.unlockedFamily(player, WeaponFamily.SWORD) && ComboSpecials.unlockedFamily(player, WeaponFamily.BOW);
			case AXE_CROSSBOW -> ComboSpecials.unlockedFamily(player, WeaponFamily.AXE) && ComboSpecials.unlockedFamily(player, WeaponFamily.BOW);
		};
	}

	private static HybridFact.Builder fact(ServerPlayer player, HybridAction action, LivingEntity target) {
		Runtime runtime = state(player);
		long now = player.level().getGameTime();
		trackAir(player, runtime, now);
		return HybridFact.builder(action, now)
				.target(target == null ? null : target.getUUID())
				.hostile(hostile(player, target))
				.aerial(runtime.air.id);
	}

	private static boolean hostile(ServerPlayer player, LivingEntity target) {
		if (target == null || !target.isAlive()) {
			return false;
		}
		if (target instanceof Player other) {
			return player.canHarmPlayer(other);
		}
		return target.getType().getCategory() == MobCategory.MONSTER;
	}

	private static boolean charged(ServerPlayer player) {
		return player.getAttackStrengthScale(0.5F) >= 0.85F;
	}

	private static boolean away(ServerPlayer player) {
		Runtime runtime = STATES.get(player.getUUID());
		return runtime != null && runtime.away > 0;
	}

	private static Distance distance(ServerPlayer player, LivingEntity target) {
		if (target == null) {
			return new Distance(false, false);
		}
		Runtime runtime = state(player);
		double now = player.distanceTo(target);
		Double previous = runtime.distances.put(target.getUUID(), now);
		if (previous == null) {
			return new Distance(false, false);
		}
		Vec3 toward = player.position().subtract(target.position());
		Vec3 motion = target.getDeltaMovement();
		boolean movingIn = motion.x * toward.x + motion.z * toward.z > 0.008;
		return new Distance(now > previous + 0.6, movingIn || now + 0.35 < previous);
	}

	private static Runtime state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> {
			Runtime created = new Runtime(config.balance());
			created.air.grounded = player.onGround() || player.isInWater();
			return created;
		});
	}

	private static void sync(ServerPlayer player, Runtime runtime, long now) {
		HybridStyleId style = runtime.engine.style();
		HybridPhase phase = runtime.engine.phase();
		int hash = phase.ordinal() * 31 + (style == null ? 0 : style.ordinal() + 1);
		if (hash == runtime.synced && phase != HybridPhase.COOLDOWN) {
			return;
		}
		runtime.synced = hash;
		ModNetworking.syncHybrid(player, new HybridStatePayload(
				style == null ? 255 : style.pair().ordinal(),
				style == null ? 255 : style.ordinal(),
				phase.ordinal(),
				runtime.engine.cooldownRemaining(now),
				runtime.engine.cooldownMax(),
				style != null && style.crown() && phase == HybridPhase.READY));
	}

	private record Distance(boolean increasing, boolean approaching) {
	}

	private static final class Air {
		private boolean grounded = true;
		private int id;
		private boolean wind;
		private boolean lunge;
		private boolean spear;
		private boolean aerial;
		private boolean lateSwap;
		private boolean pendingWind;
		private boolean pendingAerial;
		private long spearTick = -10_000L;
	}

	private static final class Runtime {
		private final HybridEngine engine;
		private final Air air = new Air();
		private final Map<UUID, Double> distances = new HashMap<>();
		private int away;
		private long chargeFavorUntil;
		private long frontalUntil;
		private int synced = Integer.MIN_VALUE;

		private Runtime(HybridBalance balance) {
			this.engine = new HybridEngine(balance);
		}
	}
}
