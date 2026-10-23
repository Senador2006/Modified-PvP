package com.example.modifiedpvp.combat.hybrid;

public enum HybridStyleId {
	EXTENDED_REACH(HybridPair.SPEAR_MACE, 1, false, "combat.modifiedpvp.hybrid.style.extended", "combat.modifiedpvp.hybrid.ready.extended"),
	CHARGE_IMPACT(HybridPair.SPEAR_MACE, 2, false, "combat.modifiedpvp.hybrid.style.charge", "combat.modifiedpvp.hybrid.ready.charge"),
	AERIAL_REACTION(HybridPair.SPEAR_MACE, 3, false, "combat.modifiedpvp.hybrid.style.aerial", "combat.modifiedpvp.hybrid.ready.aerial"),
	RELENTLESS_HUNT(HybridPair.SWORD_TRIDENT, 1, false, "combat.modifiedpvp.hybrid.style.hunt", "combat.modifiedpvp.hybrid.ready.hunt"),
	OFFENSIVE_RETURN(HybridPair.SWORD_TRIDENT, 2, false, "combat.modifiedpvp.hybrid.style.return", "combat.modifiedpvp.hybrid.ready.return"),
	COORDINATED_DUEL(HybridPair.SWORD_TRIDENT, 3, true, "combat.modifiedpvp.hybrid.style.duel", "combat.modifiedpvp.hybrid.ready.duel"),
	ANCHOR_IMPACT(HybridPair.MACE_TRIDENT, 1, false, "combat.modifiedpvp.hybrid.style.anchor", "combat.modifiedpvp.hybrid.ready.anchor"),
	GUIDED_FALL(HybridPair.MACE_TRIDENT, 2, false, "combat.modifiedpvp.hybrid.style.guided", "combat.modifiedpvp.hybrid.ready.guided"),
	THUNDER_ECHO(HybridPair.MACE_TRIDENT, 3, true, "combat.modifiedpvp.hybrid.style.echo", "combat.modifiedpvp.hybrid.ready.echo"),
	SOVEREIGN_RETALIATION(HybridPair.AXE_SHIELD, 1, false, "combat.modifiedpvp.hybrid.style.retaliation", "combat.modifiedpvp.hybrid.ready.retaliation"),
	BASTION_ADVANCE(HybridPair.AXE_SHIELD, 2, false, "combat.modifiedpvp.hybrid.style.bastion", "combat.modifiedpvp.hybrid.ready.bastion"),
	FORMATION_BREAK(HybridPair.AXE_SHIELD, 3, false, "combat.modifiedpvp.hybrid.style.formation", "combat.modifiedpvp.hybrid.ready.formation"),
	PROTECTED_THRUST(HybridPair.SWORD_SHIELD, 1, false, "combat.modifiedpvp.hybrid.style.protected", "combat.modifiedpvp.hybrid.ready.protected"),
	IMMEDIATE_RESPONSE(HybridPair.SWORD_SHIELD, 2, false, "combat.modifiedpvp.hybrid.style.immediate", "combat.modifiedpvp.hybrid.ready.immediate"),
	GUARD_CUT(HybridPair.SWORD_SHIELD, 3, false, "combat.modifiedpvp.hybrid.style.guard_cut", "combat.modifiedpvp.hybrid.ready.guard_cut"),
	ARMED_FEINT(HybridPair.SWORD_CROSSBOW, 1, false, "combat.modifiedpvp.hybrid.style.feint", "combat.modifiedpvp.hybrid.ready.feint"),
	SUDDEN_SHOT(HybridPair.SWORD_CROSSBOW, 2, false, "combat.modifiedpvp.hybrid.style.sudden", "combat.modifiedpvp.hybrid.ready.sudden"),
	LAST_RESORT(HybridPair.SWORD_CROSSBOW, 3, true, "combat.modifiedpvp.hybrid.style.resort", "combat.modifiedpvp.hybrid.ready.resort"),
	CROWN_OPENING(HybridPair.AXE_CROSSBOW, 1, false, "combat.modifiedpvp.hybrid.style.crown", "combat.modifiedpvp.hybrid.ready.crown"),
	LAST_WORD(HybridPair.AXE_CROSSBOW, 2, false, "combat.modifiedpvp.hybrid.style.last_word", "combat.modifiedpvp.hybrid.ready.last_word"),
	KING_PRESSURE(HybridPair.AXE_CROSSBOW, 3, false, "combat.modifiedpvp.hybrid.style.pressure", "combat.modifiedpvp.hybrid.ready.pressure");

	private final HybridPair pair;
	private final int priority;
	private final boolean optional;
	private final String styleKey;
	private final String readyKey;

	HybridStyleId(HybridPair pair, int priority, boolean optional, String styleKey, String readyKey) {
		this.pair = pair;
		this.priority = priority;
		this.optional = optional;
		this.styleKey = styleKey;
		this.readyKey = readyKey;
	}

	public HybridPair pair() {
		return this.pair;
	}

	public int priority() {
		return this.priority;
	}

	public boolean optional() {
		return this.optional;
	}

	public String styleKey() {
		return this.styleKey;
	}

	public String readyKey() {
		return this.readyKey;
	}

	public boolean crown() {
		return this.pair == HybridPair.AXE_CROSSBOW && !this.optional;
	}
}
