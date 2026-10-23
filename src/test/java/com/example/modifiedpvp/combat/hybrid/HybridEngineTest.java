package com.example.modifiedpvp.combat.hybrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HybridEngineTest {
	private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID PROJECTILE = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Test
	void extendedReachOutranksChargeImpact() {
		HybridEngine engine = engine();
		HybridResult result = engine.offer(smash(1, 1, true, true, true, true), EnumSet.of(HybridPair.SPEAR_MACE));
		assertEquals(HybridVerdict.PREPARED, result.verdict());
		assertEquals(HybridStyleId.EXTENDED_REACH, result.style());
	}

	@Test
	void lungeInOpenAirDoesNotPrepare() {
		HybridEngine engine = engine();
		HybridFact smash = HybridFact.builder(HybridAction.MACE_SMASH, 1)
				.hit(true)
				.airborne(true)
				.validFall(true)
				.aerial(1)
				.sequenceLunge(true)
				.sequenceSpear(true)
				.build();
		assertEquals(HybridVerdict.OBSERVED, engine.offer(smash, EnumSet.of(HybridPair.SPEAR_MACE)).verdict());
	}

	@Test
	void secondTrajectoryExecutesChargeImpact() {
		HybridEngine engine = engine();
		assertEquals(HybridVerdict.PREPARED, engine.offer(smash(1, 1, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE)).verdict());
		HybridResult sameAir = engine.offer(smash(2, 1, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE));
		assertEquals(HybridVerdict.OBSERVED, sameAir.verdict());
		HybridResult next = engine.offer(smash(3, 2, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE));
		assertEquals(HybridVerdict.EXECUTED, next.verdict());
		assertEquals(HybridStyleId.CHARGE_IMPACT, next.style());
		assertEquals(HybridPhase.COOLDOWN, engine.phase());
	}

	@Test
	void duplicateActionIdIsIgnored() {
		HybridEngine engine = engine();
		UUID action = UUID.randomUUID();
		HybridFact first = HybridFact.builder(HybridAction.MACE_SMASH, 1).actionId(action).hit(true).airborne(true).validFall(true).aerial(1).sequenceWind(true).sequenceLunge(true).build();
		HybridFact second = HybridFact.builder(HybridAction.MACE_SMASH, 2).actionId(action).hit(true).airborne(true).validFall(true).aerial(2).sequenceWind(true).sequenceLunge(true).build();
		assertEquals(HybridVerdict.PREPARED, engine.offer(first, EnumSet.of(HybridPair.SPEAR_MACE)).verdict());
		assertEquals(HybridVerdict.IGNORED, engine.offer(second, EnumSet.of(HybridPair.SPEAR_MACE)).verdict());
	}

	@Test
	void relentlessHuntNeedsANewPullBeforeTheThrust() {
		HybridEngine engine = engine();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.SWORD_TRIDENT);
		engine.offer(sword(1), pairs);
		engine.offer(tridentHit(2, true), pairs);
		assertEquals(HybridVerdict.PREPARED, engine.offer(pull(3), pairs).verdict());
		assertEquals(HybridStyleId.RELENTLESS_HUNT, engine.style());
		assertEquals(HybridVerdict.OBSERVED, engine.offer(thrust(4), pairs).verdict());
		assertEquals(HybridVerdict.PRIMED, engine.offer(pull(5), pairs).verdict());
		assertEquals(HybridVerdict.EXECUTED, engine.offer(thrust(6), pairs).verdict());
	}

	@Test
	void loyaltyWithoutPullPreparesOffensiveReturn() {
		HybridEngine engine = engine();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.SWORD_TRIDENT);
		engine.offer(HybridFact.builder(HybridAction.TRIDENT_HIT, 1).hit(true).loyalty(true).projectile(PROJECTILE).target(TARGET).build(), pairs);
		engine.offer(HybridFact.builder(HybridAction.SWORD_HIT, 2).hit(true).tridentAway(true).target(TARGET).build(), pairs);
		HybridResult prepared = engine.offer(HybridFact.builder(HybridAction.TRIDENT_RETURN, 3).hit(true).loyalty(true).projectile(PROJECTILE).build(), pairs);
		assertEquals(HybridVerdict.PREPARED, prepared.verdict());
		assertEquals(HybridStyleId.OFFENSIVE_RETURN, prepared.style());
		assertEquals(HybridVerdict.EXECUTED, engine.offer(HybridFact.builder(HybridAction.TRIDENT_THROW, 4).hit(true).build(), pairs).verdict());
	}

	@Test
	void optionalCoordinatedDuelStaysOff() {
		HybridEngine engine = engine();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.SWORD_TRIDENT);
		engine.offer(sword(1), pairs);
		engine.offer(HybridFact.builder(HybridAction.TRIDENT_HIT, 2).hit(true).target(TARGET).loyalty(true).build(), pairs);
		assertEquals(HybridVerdict.OBSERVED, engine.offer(sword(3), pairs).verdict());
		assertFalse(engine.phase() == HybridPhase.READY);
	}

	@Test
	void cooldownBlocksTheNextPreparation() {
		HybridEngine engine = engine();
		engine.offer(smash(1, 1, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE));
		engine.offer(smash(2, 2, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE));
		assertEquals(HybridPhase.COOLDOWN, engine.phase());
		assertEquals(HybridVerdict.IGNORED, engine.offer(smash(3, 3, false, true, true, false), EnumSet.of(HybridPair.SPEAR_MACE)).verdict());
	}

	@Test
	void crownOpeningPreparesThenExecutesOnTheNextHeavy() {
		HybridEngine engine = engine();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.AXE_CROSSBOW);
		engine.offer(shot(1), pairs);
		assertEquals(HybridVerdict.PREPARED, engine.offer(heavy(2, true), pairs).verdict());
		assertEquals(HybridStyleId.CROWN_OPENING, engine.style());
		assertEquals(HybridVerdict.PRIMED, engine.offer(shot(3), pairs).verdict());
		assertEquals(HybridVerdict.EXECUTED, engine.offer(heavy(4, true), pairs).verdict());
	}

	@Test
	void oneFireworkActionDoesNotCountTwice() {
		HybridEngine engine = engine();
		UUID action = UUID.randomUUID();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.SWORD_CROSSBOW);
		engine.offer(sword(1), pairs);
		HybridFact burst = HybridFact.builder(HybridAction.CROSSBOW_FIREWORK, 2).actionId(action).hit(true).affected(3).target(TARGET).build();
		engine.offer(burst, pairs);
		assertEquals(HybridVerdict.IGNORED, engine.offer(HybridFact.builder(HybridAction.CROSSBOW_FIREWORK, 3).actionId(action).hit(true).affected(3).target(TARGET).build(), pairs).verdict());
	}

	@Test
	void blockedHeavyPrefersFormationBreak() {
		HybridEngine engine = engine();
		Set<HybridPair> pairs = EnumSet.of(HybridPair.AXE_SHIELD);
		engine.offer(HybridFact.builder(HybridAction.SHIELD_BLOCK, 1).hit(true).timed(true).build(), pairs);
		HybridResult result = engine.offer(HybridFact.builder(HybridAction.AXE_HEAVY, 2).hit(true).blocked(true).fullCharge(true).target(TARGET).build(), pairs);
		assertEquals(HybridVerdict.PREPARED, result.verdict());
		assertEquals(HybridStyleId.FORMATION_BREAK, result.style());
		assertTrue(engine.claims(HybridAction.AXE_HEAVY));
	}

	private static HybridEngine engine() {
		return new HybridEngine(HybridBalance.defaults());
	}

	private static HybridFact smash(long tick, int aerial, boolean lateSwap, boolean wind, boolean lunge, boolean aerialMove) {
		return HybridFact.builder(HybridAction.MACE_SMASH, tick)
				.hit(true)
				.airborne(true)
				.validFall(true)
				.aerial(aerial)
				.lateSwap(lateSwap)
				.sequenceWind(wind)
				.sequenceLunge(lunge)
				.sequenceSpear(lunge || aerialMove)
				.sequenceAerial(aerialMove || wind)
				.target(TARGET)
				.build();
	}

	private static HybridFact sword(long tick) {
		return HybridFact.builder(HybridAction.SWORD_HIT, tick).hit(true).target(TARGET).fullCharge(true).build();
	}

	private static HybridFact thrust(long tick) {
		return HybridFact.builder(HybridAction.SWORD_THRUST, tick).hit(true).target(TARGET).build();
	}

	private static HybridFact tridentHit(long tick, boolean increasing) {
		return HybridFact.builder(HybridAction.TRIDENT_HIT, tick).hit(true).target(TARGET).loyalty(true).distanceIncreasing(increasing).projectile(PROJECTILE).build();
	}

	private static HybridFact pull(long tick) {
		return HybridFact.builder(HybridAction.PULL, tick).hit(true).target(TARGET).loyalty(true).pull(true).build();
	}

	private static HybridFact shot(long tick) {
		return HybridFact.builder(HybridAction.CROSSBOW_ARROW, tick).hit(true).target(TARGET).build();
	}

	private static HybridFact heavy(long tick, boolean full) {
		return HybridFact.builder(HybridAction.AXE_HEAVY, tick).hit(true).fullCharge(full).target(TARGET).build();
	}
}
