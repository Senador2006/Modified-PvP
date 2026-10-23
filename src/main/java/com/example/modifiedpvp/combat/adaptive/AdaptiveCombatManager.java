package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.ComboSpecials;
import com.example.modifiedpvp.network.AdaptiveStatePayload;
import com.example.modifiedpvp.network.ModNetworking;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AdaptiveCombatManager {
	private static final Map<UUID, AdaptiveCombatState> STATES = new HashMap<>();
	private static AdaptiveBalanceConfig config = AdaptiveBalanceConfig.defaults();

	private AdaptiveCombatManager() {
	}

	public static void configure(AdaptiveBalanceConfig loaded) {
		config = loaded == null ? AdaptiveBalanceConfig.defaults() : loaded;
	}

	public static AdaptiveBalanceConfig config() {
		return config;
	}

	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, AdaptiveCombatState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, AdaptiveCombatState> entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				continue;
			}
			AdaptiveCombatState state = entry.getValue();
			boolean dirty = false;
			for (WeaponFamily family : WeaponFamily.values()) {
				dirty |= tickFamily(state.family(family), family, now);
			}
			if (dirty) {
				sync(player, state, now);
			}
		}
	}

	public static AdaptiveStyle record(ServerPlayer player, WeaponFamily family, CombatActionType type, LivingEntity target, boolean hit, boolean blocked, boolean perfect, int affected, EnumSet<ContextTag> tags) {
		if (FinisherEffects.generated() || player == null) {
			return null;
		}
		if (!ComboSpecials.unlockedFamily(player, family)) {
			return null;
		}
		UUID targetId = target == null ? null : target.getUUID();
		EnumSet<ContextTag> context = tags == null ? EnumSet.noneOf(ContextTag.class) : EnumSet.copyOf(tags);
		if (target instanceof Player) {
			context.add(ContextTag.PVP);
		} else if (target != null) {
			context.add(ContextTag.PVE);
		}
		if (blocked) {
			context.add(ContextTag.BLOCKED);
		}
		long now = now(player);
		CombatActionEvent event = CombatActionEvent.builder(player.getUUID(), family, type, now)
				.target(targetId)
				.hit(hit)
				.blocked(blocked)
				.perfect(perfect)
				.affected(affected)
				.tags(context)
				.build();
		return record(player, event);
	}

	public static AdaptiveStyle record(ServerPlayer player, CombatActionEvent event) {
		if (FinisherEffects.generated() || event.has(ContextTag.FINISHER_DAMAGE)) {
			return null;
		}
		if (!ComboSpecials.unlockedFamily(player, event.family())) {
			return null;
		}
		AdaptiveCombatState state = state(player);
		FamilyCombatState family = state.family(event.family());
		long now = event.serverTick();
		tickFamily(family, event.family(), now);
		if (now < family.cooldownUntil()) {
			return null;
		}
		if (family.phase() == FamilyPhase.READY) {
			FinisherOpportunity opportunity = family.opportunity();
			if (opportunity != null && OpportunityResolver.triggerMatches(opportunity.style(), event, opportunity)) {
				boolean success = event.hitValid() || event.blocked();
				AdaptiveStyle style = opportunity.style();
				consume(player, family, event.family(), opportunity, now, success);
				sync(player, state, now);
				return success ? style : null;
			}
			if (opportunity != null && OpportunityResolver.consumeOnFailedAttempt(opportunity.style(), event) && isAttempt(event)) {
				consume(player, family, event.family(), opportunity, now, false);
				sync(player, state, now);
			}
			return null;
		}

		if (!event.hitValid() && !event.blocked() && !event.perfectTiming()) {
			return null;
		}
		if (!family.window().remember(event, config.maxWindowEvents(), now - config.windowTicks(event.family()))) {
			return null;
		}
		if (family.phase() == FamilyPhase.IDLE || family.phase() == FamilyPhase.COOLDOWN && now >= family.cooldownUntil()) {
			family.setPhase(FamilyPhase.BUILDING);
		}
		AdaptiveStyle style = OpportunityResolver.evaluate(family, event.family(), config, now);
		if (style != null && family.phase() != FamilyPhase.READY) {
			UUID focus = style.requiresSameTarget() ? event.targetId() : null;
			family.setOpportunity(OpportunityResolver.lock(event.family(), style, now, focus, player.getMainHandItem().getItem().hashCode(), config));
			family.setPhase(FamilyPhase.READY);
			FinisherEffects.cue(player, style);
			if (config.debugLog()) {
				ModifiedPvP.LOGGER.info("adaptive ready {} {} for {}", event.family(), style, player.getGameProfile().name());
			}
		} else if (family.phase() == FamilyPhase.BUILDING) {
			int percent = Math.round(bestProgress(family, event.family()) * 100.0F);
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.progress",
					Component.translatable(event.family().translationKey()), percent, 100), true);
		}
		sync(player, state, now);
		return null;
	}

	public static boolean tryConsume(ServerPlayer player, WeaponFamily family, AdaptiveStyle expected, CombatActionType type, LivingEntity target, boolean hit, boolean blocked) {
		if (!ComboSpecials.unlockedFamily(player, family)) {
			return false;
		}
		AdaptiveCombatState state = state(player);
		FamilyCombatState familyState = state.family(family);
		long now = now(player);
		tickFamily(familyState, family, now);
		FinisherOpportunity opportunity = familyState.opportunity();
		if (familyState.phase() != FamilyPhase.READY || opportunity == null || opportunity.style() != expected) {
			return false;
		}
		UUID targetId = target == null ? null : target.getUUID();
		CombatActionEvent event = CombatActionEvent.builder(player.getUUID(), family, type, now)
				.target(targetId)
				.hit(hit)
				.blocked(blocked)
				.build();
		if (!OpportunityResolver.triggerMatches(expected, event, opportunity)) {
			if (isAttempt(event) && OpportunityResolver.consumeOnFailedAttempt(expected, event)) {
				consume(player, familyState, family, opportunity, now, false);
				sync(player, state, now);
			}
			return false;
		}
		if (!hit && !blocked) {
			consume(player, familyState, family, opportunity, now, false);
			sync(player, state, now);
			return false;
		}
		consume(player, familyState, family, opportunity, now, true);
		sync(player, state, now);
		return true;
	}

	public static boolean isReady(Player player, WeaponFamily family) {
		FamilyCombatState familyState = peek(player, family);
		return familyState != null && familyState.phase() == FamilyPhase.READY && familyState.opportunity() != null;
	}

	public static boolean isReady(Player player, WeaponFamily family, AdaptiveStyle style) {
		FamilyCombatState familyState = peek(player, family);
		return familyState != null
				&& familyState.phase() == FamilyPhase.READY
				&& familyState.opportunity() != null
				&& familyState.opportunity().style() == style;
	}

	public static java.util.UUID readyTarget(Player player, WeaponFamily family) {
		FamilyCombatState familyState = peek(player, family);
		return familyState != null && familyState.opportunity() != null && familyState.phase() == FamilyPhase.READY
				? familyState.opportunity().targetId() : null;
	}

	public static AdaptiveStyle readyStyle(Player player, WeaponFamily family) {
		FamilyCombatState familyState = peek(player, family);
		return familyState != null && familyState.opportunity() != null && familyState.phase() == FamilyPhase.READY
				? familyState.opportunity().style() : null;
	}

	public static boolean buildingUninterruptible(Player player) {
		AdaptiveCombatState state = STATES.get(player.getUUID());
		if (state == null) {
			return false;
		}
		for (WeaponFamily family : WeaponFamily.values()) {
			if (!family.guardedWhileBuilding()) {
				continue;
			}
			FamilyCombatState familyState = state.family(family);
			if (familyState.phase() == FamilyPhase.BUILDING || familyState.phase() == FamilyPhase.READY) {
				return true;
			}
		}
		return false;
	}

	public static void interrupt(ServerPlayer player) {
		interrupt(player, family -> true);
	}

	public static void interrupt(ServerPlayer player, java.util.function.Predicate<WeaponFamily> include) {
		AdaptiveCombatState state = STATES.get(player.getUUID());
		if (state == null) {
			return;
		}
		boolean dirty = false;
		for (WeaponFamily family : WeaponFamily.values()) {
			if (!family.interruptible() || !include.test(family)) {
				continue;
			}
			FamilyCombatState familyState = state.family(family);
			if (familyState.phase() == FamilyPhase.BUILDING) {
				familyState.resetTransient();
				dirty = true;
			}
		}
		if (dirty) {
			FinisherEffects.feedback(player, "combat.modifiedpvp.combo.interrupted");
			sync(player, state, now(player));
		}
	}

	public static void clear(ServerPlayer player) {
		AdaptiveCombatState state = STATES.get(player.getUUID());
		if (state == null) {
			return;
		}
		state.clearTransient();
		ComboSpecials.storeFamilyCooldowns(player, state);
		sync(player, state, now(player));
	}

	public static void forget(UUID id) {
		STATES.remove(id);
	}

	public static void clearAll() {
		STATES.clear();
	}

	public static AdaptiveSnapshot snapshot(Player player) {
		AdaptiveCombatState state = STATES.get(player.getUUID());
		if (state == null) {
			return AdaptiveSnapshot.idle();
		}
		long now = player.level().getGameTime();
		AdaptiveSnapshot best = AdaptiveSnapshot.idle();
		int rank = -1;
		for (WeaponFamily family : WeaponFamily.values()) {
			FamilyCombatState familyState = state.family(family);
			if (familyState.phase() == FamilyPhase.IDLE) {
				continue;
			}
			int current = switch (familyState.phase()) {
				case READY -> 3;
				case BUILDING -> 2;
				case COOLDOWN, CONSUMED -> 1;
				case IDLE -> 0;
			};
			if (current < rank) {
				continue;
			}
			rank = current;
			int ready = familyState.opportunity() == null ? 0 : (int) Math.max(0L, familyState.opportunity().expiresAt() - now);
			int cooldown = (int) Math.max(0L, familyState.cooldownUntil() - now);
			best = new AdaptiveSnapshot(
					family,
					familyState.phase(),
					familyState.opportunity() == null ? familyState.lastStyle() : familyState.opportunity().style(),
					Math.round(bestProgress(familyState, family) * 100.0F),
					ready,
					cooldown,
					config.cooldownTicks(family));
		}
		return best;
	}

	private static boolean tickFamily(FamilyCombatState family, WeaponFamily id, long now) {
		if (family.phase() == FamilyPhase.READY && family.opportunity() != null && family.opportunity().expired(now)) {
			family.setOpportunity(null);
			family.clearEvidence();
			family.window().clear();
			family.setPhase(FamilyPhase.IDLE);
			return true;
		}
		if (family.phase() == FamilyPhase.CONSUMED) {
			family.setPhase(FamilyPhase.COOLDOWN);
			return true;
		}
		if (family.phase() == FamilyPhase.COOLDOWN && now >= family.cooldownUntil()) {
			family.setCooldownUntil(0L);
			family.setPhase(FamilyPhase.IDLE);
			return true;
		}
		if (family.phase() == FamilyPhase.BUILDING) {
			family.window().trim(config.maxWindowEvents(), now - config.windowTicks(id));
			if (family.window().size() == 0) {
				family.resetTransient();
				return true;
			}
		}
		return false;
	}

	private static void consume(ServerPlayer player, FamilyCombatState family, WeaponFamily id, FinisherOpportunity opportunity, long now, boolean success) {
		family.setPhase(FamilyPhase.CONSUMED);
		family.setConsumedAt(now);
		family.setOpportunity(null);
		family.clearEvidence();
		family.window().clear();
		long until = now + config.cooldownTicks(id);
		family.setCooldownUntil(until);
		family.setPhase(FamilyPhase.COOLDOWN);
		ComboSpecials.storeFamilyCooldown(player, id, until);
		if (success) {
			FinisherEffects.feedback(player, opportunity.style().readyKey());
		}
		if (config.debugLog()) {
			ModifiedPvP.LOGGER.info("adaptive consume {} {} success={} for {}", id, opportunity.style(), success, player.getGameProfile().name());
		}
	}

	private static boolean isAttempt(CombatActionEvent event) {
		return event.type() != CombatActionType.FINISHER_ATTEMPT || event.hitValid() || event.blocked();
	}

	private static float bestProgress(FamilyCombatState family, WeaponFamily id) {
		if (family.phase() == FamilyPhase.READY) {
			return 1.0F;
		}
		float best = 0.0F;
		float required = config.minEvidence(id);
		for (AdaptiveStyle style : AdaptiveStyle.of(id)) {
			best = Math.max(best, family.evidence(style) / required);
		}
		return Math.min(1.0F, best);
	}

	private static AdaptiveCombatState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> {
			AdaptiveCombatState created = new AdaptiveCombatState(id);
			ComboSpecials.loadFamilyCooldowns(player, created);
			long now = now(player);
			for (WeaponFamily family : WeaponFamily.values()) {
				FamilyCombatState familyState = created.family(family);
				if (now < familyState.cooldownUntil()) {
					familyState.setPhase(FamilyPhase.COOLDOWN);
				}
			}
			return created;
		});
	}

	private static FamilyCombatState peek(Player player, WeaponFamily family) {
		AdaptiveCombatState state = STATES.get(player.getUUID());
		return state == null ? null : state.family(family);
	}

	private static void sync(ServerPlayer player, AdaptiveCombatState state, long now) {
		AdaptiveSnapshot snapshot = snapshot(player);
		long fingerprint = fingerprint(snapshot);
		if (fingerprint == state.lastSentFingerprint()) {
			return;
		}
		state.setLastSentFingerprint(fingerprint);
		ModNetworking.syncAdaptive(player, AdaptiveStatePayload.of(
				snapshot.family(),
				snapshot.phase(),
				snapshot.style(),
				snapshot.progress(),
				snapshot.readyTicks(),
				snapshot.cooldownTicks(),
				snapshot.cooldownMax()));
	}

	private static long fingerprint(AdaptiveSnapshot snapshot) {
		int family = snapshot.family() == null ? 31 : snapshot.family().ordinal();
		int style = snapshot.style() == null ? 31 : snapshot.style().ordinal();
		return (((long) family) << 40)
				| (((long) snapshot.phase().ordinal()) << 32)
				| (((long) style) << 24)
				| (((long) snapshot.progress()) << 16)
				| (snapshot.readyTicks() & 0xFFL) << 8
				| (snapshot.cooldownTicks() & 0xFFL);
	}

	private static long now(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		return server == null ? player.level().getGameTime() : server.overworld().getGameTime();
	}
}
