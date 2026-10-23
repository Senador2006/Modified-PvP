package com.example.modifiedpvp.combat.hybrid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class HybridEngine {
	private final HybridBalance balance;
	private final ArrayDeque<HybridFact> facts = new ArrayDeque<>();
	private final LinkedHashSet<UUID> seen = new LinkedHashSet<>();
	private HybridPhase phase = HybridPhase.IDLE;
	private HybridStyleId style;
	private UUID targetId;
	private int aerialId = -1;
	private HybridAction followup;
	private long expiresAt;
	private long cooldownUntil;
	private boolean primed;
	private long primeUntil;

	public HybridEngine(HybridBalance balance) {
		this.balance = balance == null ? HybridBalance.defaults() : balance;
	}

	public HybridPhase phase() {
		return this.phase;
	}

	public HybridStyleId style() {
		return this.phase == HybridPhase.READY ? this.style : null;
	}

	public boolean primed() {
		return this.primed && this.phase == HybridPhase.READY;
	}

	public int cooldownRemaining(long now) {
		if (this.phase != HybridPhase.COOLDOWN) {
			return 0;
		}
		return (int) Math.max(0L, this.cooldownUntil - now);
	}

	public int cooldownMax() {
		return Math.max(1, this.balance.cooldown());
	}

	public boolean tick(long now) {
		if (this.phase == HybridPhase.READY && now > this.expiresAt) {
			clearReady();
			this.phase = HybridPhase.IDLE;
			return true;
		}
		if (this.phase == HybridPhase.COOLDOWN && now >= this.cooldownUntil) {
			this.cooldownUntil = 0L;
			this.phase = HybridPhase.IDLE;
			return true;
		}
		if (this.primed && now > this.primeUntil) {
			this.primed = false;
			return true;
		}
		return false;
	}

	public void clear() {
		this.facts.clear();
		this.seen.clear();
		clearReady();
		this.phase = HybridPhase.IDLE;
		this.cooldownUntil = 0L;
	}

	public boolean claims(HybridAction action) {
		if (this.phase != HybridPhase.READY || this.style == null || action == null) {
			return false;
		}
		if (needsPrime(this.style) && !this.primed) {
			return false;
		}
		return executes(this.style, action, this.followup);
	}

	public HybridResult offer(HybridFact fact, Set<HybridPair> equipped) {
		if (fact == null) {
			return HybridResult.IGNORED;
		}
		if (fact.actionId() != null && !remember(fact.actionId())) {
			return HybridResult.IGNORED;
		}
		long now = fact.tick();
		tick(now);
		if (this.phase == HybridPhase.COOLDOWN && now < this.cooldownUntil) {
			return HybridResult.IGNORED;
		}
		if (this.phase == HybridPhase.READY) {
			return resolveReady(fact);
		}
		if (!useful(fact)) {
			return HybridResult.IGNORED;
		}
		this.facts.addLast(fact);
		while (this.facts.size() > this.balance.maxFacts() || (!this.facts.isEmpty() && now - this.facts.peekFirst().tick() > this.balance.evidenceWindow())) {
			this.facts.removeFirst();
		}
		this.phase = this.facts.size() > 1 ? HybridPhase.CANDIDATE : HybridPhase.OBSERVING;
		HybridPrep prep = HybridRules.detect(new ArrayList<>(this.facts), equipped, this.balance);
		if (prep == null || (prep.style().optional() && !this.balance.optionalStyles())) {
			return new HybridResult(HybridVerdict.OBSERVED, null);
		}
		this.style = prep.style();
		this.targetId = prep.targetId();
		this.aerialId = prep.aerialId();
		this.followup = prep.followup();
		this.expiresAt = now + this.balance.opportunityWindow();
		this.primed = false;
		this.phase = HybridPhase.READY;
		return new HybridResult(HybridVerdict.PREPARED, prep.style());
	}

	private HybridResult resolveReady(HybridFact fact) {
		HybridRules.Step step = HybridRules.resolve(this.style, fact, this.primed, this.aerialId, this.targetId, this.followup);
		if (step == HybridRules.Step.PRIME) {
			this.primed = true;
			this.primeUntil = fact.tick() + this.balance.primeWindow();
			return new HybridResult(HybridVerdict.PRIMED, this.style);
		}
		if (step == HybridRules.Step.HIT) {
			if (!fact.hit() && !this.balance.allowMissReward() && this.style != HybridStyleId.OFFENSIVE_RETURN && this.style != HybridStyleId.BASTION_ADVANCE) {
				HybridStyleId missed = this.style;
				consume(fact.tick());
				return new HybridResult(HybridVerdict.FAILED, missed);
			}
			HybridStyleId done = this.style;
			consume(fact.tick());
			return new HybridResult(HybridVerdict.EXECUTED, done);
		}
		if (step == HybridRules.Step.MISS) {
			HybridStyleId done = this.style;
			consume(fact.tick());
			return new HybridResult(HybridVerdict.FAILED, done);
		}
		return new HybridResult(HybridVerdict.OBSERVED, this.style);
	}

	private void consume(long now) {
		this.phase = HybridPhase.ATTEMPTED;
		clearReady();
		this.facts.clear();
		this.cooldownUntil = now + this.balance.cooldown();
		this.phase = HybridPhase.COOLDOWN;
	}

	private void clearReady() {
		this.style = null;
		this.targetId = null;
		this.aerialId = -1;
		this.followup = null;
		this.primed = false;
		this.primeUntil = 0L;
		this.expiresAt = 0L;
	}

	private boolean remember(UUID actionId) {
		if (!this.seen.add(actionId)) {
			return false;
		}
		while (this.seen.size() > 128) {
			this.seen.removeFirst();
		}
		return true;
	}

	private static boolean useful(HybridFact fact) {
		return switch (fact.action()) {
			case WEAPON_SWAP -> false;
			case TRIDENT_THROW, TRIDENT_RETURN, PULL, GUARD_ADVANCE, SHIELD_BLOCK -> true;
			default -> fact.hit() || fact.blocked();
		};
	}

	private static boolean needsPrime(HybridStyleId style) {
		return style == HybridStyleId.RELENTLESS_HUNT
				|| style == HybridStyleId.COORDINATED_DUEL
				|| style == HybridStyleId.ANCHOR_IMPACT
				|| style == HybridStyleId.GUIDED_FALL
				|| style == HybridStyleId.SOVEREIGN_RETALIATION
				|| style == HybridStyleId.IMMEDIATE_RESPONSE
				|| style == HybridStyleId.PROTECTED_THRUST
				|| style == HybridStyleId.GUARD_CUT
				|| style == HybridStyleId.ARMED_FEINT
				|| style == HybridStyleId.CROWN_OPENING;
	}

	private static boolean executes(HybridStyleId style, HybridAction action, HybridAction followup) {
		return switch (style) {
			case EXTENDED_REACH, CHARGE_IMPACT, AERIAL_REACTION, GUIDED_FALL, THUNDER_ECHO -> action == HybridAction.MACE_SMASH;
			case RELENTLESS_HUNT, COORDINATED_DUEL, PROTECTED_THRUST, ARMED_FEINT -> action == HybridAction.SWORD_THRUST;
			case OFFENSIVE_RETURN -> action == HybridAction.TRIDENT_THROW;
			case ANCHOR_IMPACT -> action == HybridAction.MACE_HIT || action == HybridAction.MACE_SMASH;
			case SOVEREIGN_RETALIATION, FORMATION_BREAK, CROWN_OPENING -> action == HybridAction.AXE_HEAVY;
			case BASTION_ADVANCE -> action == HybridAction.GUARD_ADVANCE;
			case IMMEDIATE_RESPONSE, GUARD_CUT -> action == HybridAction.SWORD_HIT;
			case SUDDEN_SHOT -> action == HybridAction.CROSSBOW_ARROW;
			case LAST_RESORT -> action == HybridAction.CROSSBOW_FIREWORK;
			case LAST_WORD -> action == HybridAction.CROSSBOW_ARROW || action == HybridAction.CROSSBOW_FIREWORK;
			case KING_PRESSURE -> followup != null && action == followup;
		};
	}
}
