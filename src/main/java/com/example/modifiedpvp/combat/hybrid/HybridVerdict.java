package com.example.modifiedpvp.combat.hybrid;

public enum HybridVerdict {
	IGNORED,
	OBSERVED,
	PREPARED,
	PRIMED,
	EXECUTED,
	FAILED;

	public boolean exclusive() {
		return this == EXECUTED || this == FAILED;
	}
}
