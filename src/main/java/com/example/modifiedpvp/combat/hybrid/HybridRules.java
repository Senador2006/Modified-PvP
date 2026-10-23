package com.example.modifiedpvp.combat.hybrid;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class HybridRules {
	public enum Step {
		NONE,
		PRIME,
		HIT,
		MISS
	}

	private HybridRules() {
	}

	public static HybridPrep detect(List<HybridFact> facts, Set<HybridPair> equipped, HybridBalance balance) {
		HybridPrep best = null;
		if (equipped.contains(HybridPair.SPEAR_MACE)) {
			best = better(best, spearMace(facts));
		}
		if (equipped.contains(HybridPair.SWORD_TRIDENT)) {
			best = better(best, swordTrident(facts, balance));
		}
		if (equipped.contains(HybridPair.MACE_TRIDENT)) {
			best = better(best, maceTrident(facts, balance));
		}
		if (equipped.contains(HybridPair.AXE_SHIELD)) {
			best = better(best, axeShield(facts));
		}
		if (equipped.contains(HybridPair.SWORD_SHIELD)) {
			best = better(best, swordShield(facts));
		}
		if (equipped.contains(HybridPair.SWORD_CROSSBOW)) {
			best = better(best, swordCrossbow(facts, balance));
		}
		if (equipped.contains(HybridPair.AXE_CROSSBOW)) {
			best = better(best, axeCrossbow(facts));
		}
		return best;
	}

	public static Step resolve(HybridStyleId style, HybridFact fact, boolean primed, int prepAerial, UUID target, HybridAction followup) {
		return switch (style) {
			case EXTENDED_REACH -> smashStep(fact, prepAerial, fact.lateSwap() && fact.validFall());
			case CHARGE_IMPACT -> smashStep(fact, prepAerial, fact.validFall() && fact.sequenceWind() && fact.sequenceLunge());
			case AERIAL_REACTION -> smashStep(fact, prepAerial, fact.sequenceSpear() && fact.sequenceAerial());
			case RELENTLESS_HUNT -> primedThen(fact, primed, target, HybridAction.PULL, fact.hasPull() && fact.loyalty(), HybridAction.SWORD_THRUST, fact.hit());
			case OFFENSIVE_RETURN -> fact.action() == HybridAction.TRIDENT_THROW ? Step.HIT : Step.NONE;
			case COORDINATED_DUEL -> primedThen(fact, primed, target, HybridAction.TRIDENT_THROW, true, HybridAction.SWORD_THRUST, fact.hit());
			case ANCHOR_IMPACT -> primedThen(fact, primed, target, fact.action() == HybridAction.PULL, fact.hasPull() && fact.loyalty(), maceHit(fact), fact.hit());
			case GUIDED_FALL -> primedThen(fact, primed, target, fact.action() == HybridAction.TRIDENT_HIT, fact.hit() && fact.airborne(), fact.action() == HybridAction.MACE_SMASH, fact.hit() && fact.airborne() && fact.validFall());
			case THUNDER_ECHO -> fact.action() == HybridAction.MACE_SMASH && fact.hit() ? Step.HIT : Step.NONE;
			case SOVEREIGN_RETALIATION -> primedThen(fact, primed, null, HybridAction.SHIELD_BLOCK, fact.hit(), HybridAction.AXE_HEAVY, fact.hit() && fact.fullCharge());
			case BASTION_ADVANCE -> fact.action() == HybridAction.GUARD_ADVANCE ? Step.HIT : Step.NONE;
			case FORMATION_BREAK -> heavyStep(fact, fact.blocked() && fact.fullCharge());
			case IMMEDIATE_RESPONSE -> primedThen(fact, primed, null, HybridAction.SHIELD_BLOCK, fact.hit() && fact.timed(), HybridAction.SWORD_HIT, fact.hit() && fact.fullCharge());
			case PROTECTED_THRUST -> primedThen(fact, primed, null, HybridAction.SHIELD_BLOCK, fact.hit(), HybridAction.SWORD_THRUST, fact.hit());
			case GUARD_CUT -> primedThen(fact, primed, null, HybridAction.SHIELD_BLOCK, fact.hit(), HybridAction.SWORD_HIT, fact.hit() && fact.fullCharge());
			case ARMED_FEINT -> primedThen(fact, primed, target, crossbowHit(fact), fact.hit(), fact.action() == HybridAction.SWORD_THRUST, fact.hit());
			case SUDDEN_SHOT -> arrowStep(fact, target, fact.approaching());
			case LAST_RESORT -> fact.action() == HybridAction.CROSSBOW_FIREWORK && fact.hit() ? Step.HIT : Step.NONE;
			case CROWN_OPENING -> primedThen(fact, primed, target, crossbowHit(fact), fact.hit(), fact.action() == HybridAction.AXE_HEAVY, fact.hit() && fact.fullCharge());
			case LAST_WORD -> crossbowResponse(fact, target);
			case KING_PRESSURE -> followup != null && fact.action() == followup && fact.hit() ? Step.HIT : followup != null && fact.action() == followup && fact.attempt() ? Step.MISS : Step.NONE;
		};
	}

	private static HybridPrep spearMace(List<HybridFact> facts) {
		HybridFact smash = latest(facts, fact -> fact.action() == HybridAction.MACE_SMASH && fact.hit() && fact.airborne() && fact.validFall());
		if (smash == null) {
			return null;
		}
		if (smash.lateSwap()) {
			return prep(HybridStyleId.EXTENDED_REACH, smash, null);
		}
		if (smash.sequenceWind() && smash.sequenceLunge()) {
			return prep(HybridStyleId.CHARGE_IMPACT, smash, null);
		}
		if (smash.sequenceSpear() && smash.sequenceAerial()) {
			return prep(HybridStyleId.AERIAL_REACTION, smash, null);
		}
		return null;
	}

	private static HybridPrep swordTrident(List<HybridFact> facts, HybridBalance balance) {
		HybridPrep hunt = relentless(facts);
		if (hunt != null) {
			return hunt;
		}
		HybridPrep returned = offensiveReturn(facts);
		if (returned != null) {
			return returned;
		}
		return balance.optionalStyles() ? coordinated(facts) : null;
	}

	private static HybridPrep relentless(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact pull = facts.get(index);
			if (pull.action() != HybridAction.PULL || !pull.hasPull() || !pull.loyalty() || pull.targetId() == null) {
				continue;
			}
			int trident = before(facts, index, fact -> fact.action() == HybridAction.TRIDENT_HIT && fact.hit() && fact.loyalty() && fact.distanceIncreasing() && fact.sameTarget(pull.targetId()));
			if (trident < 0) {
				continue;
			}
			int sword = before(facts, trident, fact -> fact.action() == HybridAction.SWORD_HIT && fact.hit() && fact.sameTarget(pull.targetId()));
			if (sword >= 0) {
				return prep(HybridStyleId.RELENTLESS_HUNT, pull, null);
			}
		}
		return null;
	}

	private static HybridPrep offensiveReturn(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact returned = facts.get(index);
			if (returned.action() != HybridAction.TRIDENT_RETURN || !returned.loyalty() || returned.projectileId() == null) {
				continue;
			}
			int sword = before(facts, index, fact -> (fact.action() == HybridAction.SWORD_HIT || fact.action() == HybridAction.SWORD_THRUST) && fact.hit() && fact.tridentAway());
			if (sword < 0) {
				continue;
			}
			int thrown = before(facts, sword, fact -> fact.action() == HybridAction.TRIDENT_HIT && fact.hit() && fact.loyalty() && returned.projectileId().equals(fact.projectileId()));
			if (thrown >= 0) {
				return prep(HybridStyleId.OFFENSIVE_RETURN, returned, null);
			}
		}
		return null;
	}

	private static HybridPrep coordinated(List<HybridFact> facts) {
		UUID target = null;
		int sword = 0;
		int trident = 0;
		HybridAction previous = null;
		for (HybridFact fact : facts) {
			if (!fact.hit() || fact.targetId() == null) {
				continue;
			}
			if (target == null) {
				target = fact.targetId();
			}
			if (!fact.sameTarget(target)) {
				continue;
			}
			boolean swordHit = fact.action() == HybridAction.SWORD_HIT || fact.action() == HybridAction.SWORD_THRUST;
			boolean tridentHit = fact.action() == HybridAction.TRIDENT_HIT;
			if (!swordHit && !tridentHit) {
				continue;
			}
			if (swordHit) {
				sword++;
			} else {
				trident++;
			}
			if (previous != null && previous != fact.action() && sword >= 1 && trident >= 1 && sword + trident >= 3) {
				return prep(HybridStyleId.COORDINATED_DUEL, fact, null);
			}
			previous = fact.action();
		}
		return null;
	}

	private static HybridPrep maceTrident(List<HybridFact> facts, HybridBalance balance) {
		HybridPrep anchor = anchor(facts);
		if (anchor != null) {
			return anchor;
		}
		HybridPrep guided = guided(facts);
		if (guided != null) {
			return guided;
		}
		return balance.optionalStyles() ? echo(facts) : null;
	}

	private static HybridPrep anchor(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact mace = facts.get(index);
			if (!maceHit(mace) || !mace.hit() || mace.targetId() == null) {
				continue;
			}
			int pull = before(facts, index, fact -> fact.action() == HybridAction.PULL && fact.hasPull() && fact.loyalty() && fact.sameTarget(mace.targetId()));
			if (pull < 0) {
				continue;
			}
			int trident = before(facts, pull, fact -> fact.action() == HybridAction.TRIDENT_HIT && fact.hit() && fact.loyalty() && fact.sameTarget(mace.targetId()));
			if (trident >= 0) {
				return prep(HybridStyleId.ANCHOR_IMPACT, mace, null);
			}
		}
		return null;
	}

	private static HybridPrep guided(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact smash = facts.get(index);
			if (smash.action() != HybridAction.MACE_SMASH || !smash.hit() || !smash.airborne() || !smash.validFall() || smash.targetId() == null) {
				continue;
			}
			if (!smash.sequenceWind() && !smash.sequenceAerial()) {
				continue;
			}
			int trident = before(facts, index, fact -> fact.action() == HybridAction.TRIDENT_HIT && fact.hit() && fact.airborne() && fact.aerialId() == smash.aerialId() && fact.sameTarget(smash.targetId()));
			if (trident >= 0) {
				return prep(HybridStyleId.GUIDED_FALL, smash, null);
			}
		}
		return null;
	}

	private static HybridPrep echo(List<HybridFact> facts) {
		boolean trident = false;
		boolean mace = false;
		HybridFact last = null;
		for (HybridFact fact : facts) {
			if (fact.action() == HybridAction.TRIDENT_HIT && fact.hit()) {
				trident = true;
				last = fact;
			}
			if (maceHit(fact) && fact.hit()) {
				mace = true;
				last = fact;
			}
		}
		if (trident && mace && last != null && last.affected() >= 2) {
			return prep(HybridStyleId.THUNDER_ECHO, last, null);
		}
		return null;
	}

	private static HybridPrep axeShield(List<HybridFact> facts) {
		int blocks = 0;
		boolean timed = false;
		boolean advancing = false;
		HybridFact heavy = null;
		HybridFact blockedHeavy = null;
		for (HybridFact fact : facts) {
			if (fact.action() == HybridAction.SHIELD_BLOCK && fact.hit()) {
				blocks++;
				timed |= fact.timed();
				advancing |= fact.advancing();
			}
			if (fact.action() == HybridAction.AXE_HEAVY && fact.hit()) {
				heavy = fact;
				if (fact.blocked()) {
					blockedHeavy = fact;
				}
			}
		}
		if (blockedHeavy != null && blocks > 0) {
			return prep(HybridStyleId.FORMATION_BREAK, blockedHeavy, null);
		}
		if (heavy != null && !heavy.blocked() && blocks > 0 && (timed || blocks >= 2)) {
			return prep(HybridStyleId.SOVEREIGN_RETALIATION, heavy, null);
		}
		if (blocks >= 2 && advancing) {
			HybridFact lastBlock = latest(facts, fact -> fact.action() == HybridAction.SHIELD_BLOCK && fact.hit());
			return lastBlock == null ? null : prep(HybridStyleId.BASTION_ADVANCE, lastBlock, null);
		}
		return null;
	}

	private static HybridPrep swordShield(List<HybridFact> facts) {
		boolean blocked = false;
		boolean timed = false;
		HybridFact thrust = null;
		HybridFact cut = null;
		for (HybridFact fact : facts) {
			if (fact.action() == HybridAction.SHIELD_BLOCK && fact.hit()) {
				blocked = true;
				timed |= fact.timed();
			}
			if (!blocked || !fact.hit()) {
				continue;
			}
			if (fact.action() == HybridAction.SWORD_THRUST) {
				thrust = fact;
			} else if (fact.action() == HybridAction.SWORD_HIT && fact.fullCharge()) {
				cut = fact;
			}
		}
		if (thrust != null) {
			return prep(HybridStyleId.PROTECTED_THRUST, thrust, null);
		}
		if (timed && cut != null) {
			return prep(HybridStyleId.IMMEDIATE_RESPONSE, cut, null);
		}
		if (cut != null) {
			return prep(HybridStyleId.GUARD_CUT, cut, null);
		}
		return null;
	}

	private static HybridPrep swordCrossbow(List<HybridFact> facts, HybridBalance balance) {
		HybridPrep feint = feint(facts);
		if (feint != null) {
			return feint;
		}
		HybridPrep sudden = sudden(facts);
		if (sudden != null) {
			return sudden;
		}
		return balance.optionalStyles() ? resort(facts) : null;
	}

	private static HybridPrep feint(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact melee = facts.get(index);
			if (melee.action() != HybridAction.SWORD_HIT && melee.action() != HybridAction.SWORD_THRUST || !melee.hit() || melee.targetId() == null) {
				continue;
			}
			int shot = before(facts, index, fact -> crossbowHit(fact) && fact.hit() && fact.sameTarget(melee.targetId()));
			if (shot < 0) {
				continue;
			}
			int sword = before(facts, shot, fact -> fact.action() == HybridAction.SWORD_HIT && fact.hit() && fact.sameTarget(melee.targetId()));
			if (sword >= 0) {
				return prep(HybridStyleId.ARMED_FEINT, melee, null);
			}
		}
		return null;
	}

	private static HybridPrep sudden(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact arrow = facts.get(index);
			if (arrow.action() != HybridAction.CROSSBOW_ARROW || !arrow.hit() || !arrow.approaching() || arrow.targetId() == null) {
				continue;
			}
			int sword = before(facts, index, fact -> fact.action() == HybridAction.SWORD_HIT && fact.hit() && fact.sameTarget(arrow.targetId()));
			if (sword >= 0) {
				return prep(HybridStyleId.SUDDEN_SHOT, arrow, null);
			}
		}
		return null;
	}

	private static HybridPrep resort(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact burst = facts.get(index);
			if (burst.action() != HybridAction.CROSSBOW_FIREWORK || !burst.hit() || burst.affected() < 1) {
				continue;
			}
			int sword = before(facts, index, fact -> fact.action() == HybridAction.SWORD_HIT && fact.hit());
			if (sword >= 0) {
				return prep(HybridStyleId.LAST_RESORT, burst, null);
			}
		}
		return null;
	}

	private static HybridPrep axeCrossbow(List<HybridFact> facts) {
		HybridPrep crown = crown(facts);
		if (crown != null) {
			return crown;
		}
		HybridPrep word = lastWord(facts);
		if (word != null) {
			return word;
		}
		return king(facts);
	}

	private static HybridPrep crown(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact heavy = facts.get(index);
			if (heavy.action() != HybridAction.AXE_HEAVY || !heavy.hit() || !heavy.fullCharge() || heavy.targetId() == null) {
				continue;
			}
			int shot = before(facts, index, fact -> crossbowHit(fact) && fact.hit() && fact.sameTarget(heavy.targetId()));
			if (shot >= 0) {
				return prep(HybridStyleId.CROWN_OPENING, heavy, HybridAction.AXE_HEAVY);
			}
		}
		return null;
	}

	private static HybridPrep lastWord(List<HybridFact> facts) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			HybridFact shot = facts.get(index);
			if (!crossbowHit(shot) || !shot.hit()) {
				continue;
			}
			boolean response = shot.action() == HybridAction.CROSSBOW_FIREWORK || shot.approaching();
			if (!response) {
				continue;
			}
			int heavy = before(facts, index, fact -> fact.action() == HybridAction.AXE_HEAVY && fact.hit());
			if (heavy >= 0) {
				return prep(HybridStyleId.LAST_WORD, shot, shot.action());
			}
		}
		return null;
	}

	private static HybridPrep king(List<HybridFact> facts) {
		HybridFact heavy = latest(facts, fact -> fact.action() == HybridAction.AXE_HEAVY && fact.hit());
		HybridFact shot = latest(facts, fact -> crossbowHit(fact) && fact.hit());
		if (heavy == null || shot == null) {
			return null;
		}
		HybridFact last = heavy.tick() >= shot.tick() ? heavy : shot;
		HybridAction followup = last.action() == HybridAction.AXE_HEAVY ? HybridAction.CROSSBOW_ARROW : HybridAction.AXE_HEAVY;
		return prep(HybridStyleId.KING_PRESSURE, last, followup);
	}

	private static Step smashStep(HybridFact fact, int prepAerial, boolean qualified) {
		if (fact.action() != HybridAction.MACE_SMASH || !fact.airborne() || fact.aerialId() == prepAerial) {
			return Step.NONE;
		}
		if (!fact.hit()) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		return qualified ? Step.HIT : Step.MISS;
	}

	private static Step heavyStep(HybridFact fact, boolean qualified) {
		if (fact.action() != HybridAction.AXE_HEAVY) {
			return Step.NONE;
		}
		if (!fact.hit()) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		return qualified ? Step.HIT : Step.MISS;
	}

	private static Step arrowStep(HybridFact fact, UUID target, boolean qualified) {
		if (fact.action() != HybridAction.CROSSBOW_ARROW) {
			return Step.NONE;
		}
		if (target != null && !fact.sameTarget(target)) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		if (!fact.hit()) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		return qualified ? Step.HIT : Step.MISS;
	}

	private static Step crossbowResponse(HybridFact fact, UUID target) {
		if (fact.action() != HybridAction.CROSSBOW_ARROW && fact.action() != HybridAction.CROSSBOW_FIREWORK) {
			return Step.NONE;
		}
		if (target != null && fact.targetId() != null && !fact.sameTarget(target)) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		if (!fact.hit() && fact.action() != HybridAction.CROSSBOW_FIREWORK) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		if (fact.action() == HybridAction.CROSSBOW_ARROW && !fact.approaching()) {
			return Step.MISS;
		}
		return Step.HIT;
	}

	private static Step primedThen(HybridFact fact, boolean primed, UUID target, HybridAction primer, boolean primerOk, HybridAction execute, boolean executeOk) {
		return primedThen(fact, primed, target, fact.action() == primer, primerOk, fact.action() == execute, executeOk);
	}

	private static Step primedThen(HybridFact fact, boolean primed, UUID target, boolean primerAction, boolean primerOk, boolean executeAction, boolean executeOk) {
		if (!primed && primerAction) {
			if (target != null && fact.targetId() != null && !fact.sameTarget(target)) {
				return Step.NONE;
			}
			return primerOk ? Step.PRIME : Step.NONE;
		}
		if (!executeAction) {
			return Step.NONE;
		}
		if (target != null && fact.targetId() != null && !fact.sameTarget(target)) {
			return fact.attempt() ? Step.MISS : Step.NONE;
		}
		if (!primed) {
			return Step.NONE;
		}
		if (!fact.hit() && fact.attempt()) {
			return Step.MISS;
		}
		return executeOk ? Step.HIT : fact.attempt() ? Step.MISS : Step.NONE;
	}

	private static boolean maceHit(HybridFact fact) {
		return fact.action() == HybridAction.MACE_HIT || fact.action() == HybridAction.MACE_SMASH;
	}

	private static boolean crossbowHit(HybridFact fact) {
		return fact.action() == HybridAction.CROSSBOW_ARROW || fact.action() == HybridAction.CROSSBOW_FIREWORK;
	}

	private static HybridFact latest(List<HybridFact> facts, Predicate<HybridFact> test) {
		for (int index = facts.size() - 1; index >= 0; index--) {
			if (test.test(facts.get(index))) {
				return facts.get(index);
			}
		}
		return null;
	}

	private static int before(List<HybridFact> facts, int index, Predicate<HybridFact> test) {
		for (int cursor = index - 1; cursor >= 0; cursor--) {
			if (test.test(facts.get(cursor))) {
				return cursor;
			}
		}
		return -1;
	}

	private static HybridPrep prep(HybridStyleId style, HybridFact fact, HybridAction followup) {
		return new HybridPrep(style, fact.targetId(), fact.aerialId(), followup);
	}

	private static HybridPrep better(HybridPrep current, HybridPrep candidate) {
		if (candidate == null) {
			return current;
		}
		if (current == null) {
			return candidate;
		}
		int priority = Integer.compare(current.style().priority(), candidate.style().priority());
		if (priority != 0) {
			return priority < 0 ? current : candidate;
		}
		return current.style().pair().ordinal() <= candidate.style().pair().ordinal() ? current : candidate;
	}
}
