package com.example.modifiedpvp.combat.hybrid;

public record HybridResult(HybridVerdict verdict, HybridStyleId style) {
	public static final HybridResult IGNORED = new HybridResult(HybridVerdict.IGNORED, null);

	public boolean exclusive() {
		return this.verdict.exclusive();
	}
}
