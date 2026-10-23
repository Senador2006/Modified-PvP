package com.example.modifiedpvp.combat.hybrid;

public record HybridBalance(
		int evidenceWindow,
		int opportunityWindow,
		int primeWindow,
		int cooldown,
		int maxFacts,
		boolean optionalStyles,
		boolean allowMissReward
) {
	public static HybridBalance defaults() {
		return new HybridBalance(80, 50, 30, 200, 16, false, false);
	}
}
