package com.example.modifiedpvp.network;

public enum CombatAction {
	DASH(0),
	DOUBLE_JUMP(1),
	SLIDE_START(2),
	SLIDE_STOP(3),
	THRUST(4),
	HEAVY_START(5),
	HEAVY_RELEASE(6),
	HEAVY_CANCEL(7),
	CYCLE_ARROW(8),
	PARRY(9);

	private final int id;

	CombatAction(int id) {
		this.id = id;
	}

	public int id() {
		return this.id;
	}

	public static CombatAction byId(int id) {
		for (CombatAction action : values()) {
			if (action.id == id) {
				return action;
			}
		}
		return null;
	}
}
