package com.example.modifiedpvp.combat.adaptive;

public enum WeaponFamily {
	SWORD,
	DUAL_SWORD,
	AXE,
	MACE,
	SPEAR,
	TRIDENT,
	BOW,
	CROSSBOW,
	SHIELD;

	public String translationKey() {
		return "combat.modifiedpvp.family." + this.name().toLowerCase();
	}

	public boolean interruptible() {
		return this == SWORD || this == DUAL_SWORD || this == BOW || this == SHIELD || this == CROSSBOW;
	}

	public boolean guardedWhileBuilding() {
		return this == AXE || this == MACE || this == TRIDENT;
	}
}
