package com.example.modifiedpvp.client;

public final class HybridInput {
	private static boolean guard;

	private HybridInput() {
	}

	public static void setGuard(boolean held) {
		guard = held;
	}

	public static boolean guard() {
		return guard;
	}
}
