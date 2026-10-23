package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.hybrid.HybridPhase;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class HybridHud {
	private HybridHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ModifiedPvP.id("hybrid_combat"), HybridHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui || client.screen != null || !HybridClient.visible() || HybridClient.style() == null) {
			return;
		}
		int width = 72;
		int x = 8;
		int y = graphics.guiHeight() - 54;
		int color = HybridClient.crown() ? 0xFFD4A24C : HybridClient.phase() == HybridPhase.READY ? 0xFFE8C36A : 0xFF8A8A8A;
		int fill = HybridClient.phase() == HybridPhase.READY
				? width
				: Math.round(width * (1.0F - HybridClient.cooldown() / (float) HybridClient.cooldownMax()));
		graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xC0101014);
		graphics.fill(x, y, x + width, y + 3, 0xFF3A3A40);
		if (fill > 0) {
			graphics.fill(x, y, x + Math.min(width, fill), y + 3, color);
		}
		Component label = Component.translatable(HybridClient.phase() == HybridPhase.READY ? HybridClient.style().readyKey() : HybridClient.style().styleKey());
		graphics.drawString(client.font, label, x + (HybridClient.crown() ? 8 : 0), y - 10, color, true);
		if (HybridClient.crown()) {
			graphics.fill(x, y - 8, x + 5, y - 7, 0xFFE2B340);
			graphics.fill(x, y - 11, x + 1, y - 7, 0xFFE2B340);
			graphics.fill(x + 2, y - 12, x + 3, y - 7, 0xFFB4333A);
			graphics.fill(x + 4, y - 11, x + 5, y - 7, 0xFFE2B340);
		}
	}
}
