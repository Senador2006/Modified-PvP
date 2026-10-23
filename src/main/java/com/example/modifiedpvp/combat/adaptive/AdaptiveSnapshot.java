package com.example.modifiedpvp.combat.adaptive;

public record AdaptiveSnapshot(
		WeaponFamily family,
		FamilyPhase phase,
		AdaptiveStyle style,
		int progress,
		int readyTicks,
		int cooldownTicks,
		int cooldownMax
) {
	public static AdaptiveSnapshot idle() {
		return new AdaptiveSnapshot(null, FamilyPhase.IDLE, null, 0, 0, 0, 1);
	}

	public boolean visible() {
		return this.family != null && this.phase != FamilyPhase.IDLE;
	}
}
