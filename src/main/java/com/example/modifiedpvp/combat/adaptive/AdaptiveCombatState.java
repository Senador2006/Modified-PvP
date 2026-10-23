package com.example.modifiedpvp.combat.adaptive;

import java.util.EnumMap;
import java.util.UUID;

public final class AdaptiveCombatState {
	private final UUID playerId;
	private final EnumMap<WeaponFamily, FamilyCombatState> families = new EnumMap<>(WeaponFamily.class);
	private long lastSentFingerprint;

	public AdaptiveCombatState(UUID playerId) {
		this.playerId = playerId;
		for (WeaponFamily family : WeaponFamily.values()) {
			this.families.put(family, new FamilyCombatState());
		}
	}

	public UUID playerId() {
		return this.playerId;
	}

	public FamilyCombatState family(WeaponFamily family) {
		return this.families.get(family);
	}

	public void clearTransient() {
		for (FamilyCombatState state : this.families.values()) {
			state.resetTransient();
		}
	}

	public long lastSentFingerprint() {
		return this.lastSentFingerprint;
	}

	public void setLastSentFingerprint(long fingerprint) {
		this.lastSentFingerprint = fingerprint;
	}
}
