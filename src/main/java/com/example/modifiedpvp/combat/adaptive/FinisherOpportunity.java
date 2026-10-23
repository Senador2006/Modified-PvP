package com.example.modifiedpvp.combat.adaptive;

import java.util.UUID;

public final class FinisherOpportunity {
	private final WeaponFamily family;
	private final AdaptiveStyle style;
	private final long expiresAt;
	private final UUID targetId;
	private final int sourceItem;

	public FinisherOpportunity(WeaponFamily family, AdaptiveStyle style, long expiresAt, UUID targetId, int sourceItem) {
		this.family = family;
		this.style = style;
		this.expiresAt = expiresAt;
		this.targetId = targetId;
		this.sourceItem = sourceItem;
	}

	public WeaponFamily family() {
		return this.family;
	}

	public AdaptiveStyle style() {
		return this.style;
	}

	public long expiresAt() {
		return this.expiresAt;
	}

	public UUID targetId() {
		return this.targetId;
	}

	public int sourceItem() {
		return this.sourceItem;
	}

	public boolean expired(long now) {
		return now >= this.expiresAt;
	}

	public boolean matchesTarget(UUID actual) {
		if (!this.style.requiresSameTarget()) {
			return true;
		}
		return this.targetId != null && this.targetId.equals(actual);
	}
}
