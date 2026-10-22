package com.example.modifiedpvp.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ParryImpact {
	private static int remaining;
	private static int flashFrames;
	private static boolean heldFreeze;

	private ParryImpact() {
	}

	public static void start(int ticks) {
		if (ticks <= 0) {
			return;
		}
		remaining = Math.max(remaining, ticks);
		flashFrames = 2;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null && !heldFreeze && !minecraft.level.tickRateManager().isFrozen()) {
			minecraft.level.tickRateManager().setFrozen(true);
			heldFreeze = true;
		}
	}

	public static void tick() {
		if (remaining <= 0) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (heldFreeze && minecraft.level != null) {
			minecraft.level.tickRateManager().setFrozen(true);
		}
		if (--remaining <= 0) {
			flashFrames = 0;
			release();
		}
	}

	public static void clear() {
		remaining = 0;
		flashFrames = 0;
		release();
	}

	public static boolean frozen() {
		return remaining > 0;
	}

	public static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		if (flashFrames <= 0 && remaining <= 0) {
			return;
		}
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		if (flashFrames > 0) {
			int alpha = flashFrames >= 2 ? 0xE6 : 0x78;
			graphics.fill(0, 0, width, height, (alpha << 24) | 0xFFFFFF);
			flashFrames--;
			return;
		}
		graphics.fill(0, 0, width, height, 0x20FFFFFF);
	}

	private static void release() {
		if (!heldFreeze) {
			return;
		}
		heldFreeze = false;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			minecraft.level.tickRateManager().setFrozen(false);
		}
	}
}
