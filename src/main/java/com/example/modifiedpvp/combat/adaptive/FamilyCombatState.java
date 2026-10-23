package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumMap;

public final class FamilyCombatState {
	private FamilyPhase phase = FamilyPhase.IDLE;
	private final RecentActionWindow window = new RecentActionWindow();
	private final EnumMap<AdaptiveStyle, Float> evidence = new EnumMap<>(AdaptiveStyle.class);
	private FinisherOpportunity opportunity;
	private AdaptiveStyle lastStyle;
	private long cooldownUntil;
	private long consumedAt;

	public FamilyPhase phase() {
		return this.phase;
	}

	public void setPhase(FamilyPhase phase) {
		this.phase = phase;
	}

	public RecentActionWindow window() {
		return this.window;
	}

	public float evidence(AdaptiveStyle style) {
		return this.evidence.getOrDefault(style, 0.0F);
	}

	public void setEvidence(AdaptiveStyle style, float value) {
		if (value <= 0.0F) {
			this.evidence.remove(style);
		} else {
			this.evidence.put(style, value);
		}
	}

	public void clearEvidence() {
		this.evidence.clear();
	}

	public FinisherOpportunity opportunity() {
		return this.opportunity;
	}

	public void setOpportunity(FinisherOpportunity opportunity) {
		this.opportunity = opportunity;
		if (opportunity != null) {
			this.lastStyle = opportunity.style();
		}
	}

	public AdaptiveStyle lastStyle() {
		return this.lastStyle;
	}

	public long cooldownUntil() {
		return this.cooldownUntil;
	}

	public void setCooldownUntil(long until) {
		this.cooldownUntil = until;
	}

	public long consumedAt() {
		return this.consumedAt;
	}

	public void setConsumedAt(long tick) {
		this.consumedAt = tick;
	}

	public float progress(AdaptiveBalanceConfig config) {
		float best = 0.0F;
		for (AdaptiveStyle style : AdaptiveStyle.of(this.familyOfLast())) {
			best = Math.max(best, this.evidence(style) / config.minEvidence(style.family()));
		}
		return Math.min(1.0F, best);
	}

	public void resetTransient() {
		this.phase = this.cooldownUntil > 0L ? FamilyPhase.COOLDOWN : FamilyPhase.IDLE;
		this.window.clear();
		this.evidence.clear();
		this.opportunity = null;
		this.consumedAt = 0L;
	}

	private WeaponFamily familyOfLast() {
		if (this.opportunity != null) {
			return this.opportunity.family();
		}
		if (this.lastStyle != null) {
			return this.lastStyle.family();
		}
		return WeaponFamily.SWORD;
	}
}
