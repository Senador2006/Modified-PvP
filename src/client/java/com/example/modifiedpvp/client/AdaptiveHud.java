package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.adaptive.FamilyPhase;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class AdaptiveHud {
	private AdaptiveHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ModifiedPvP.id("adaptive_combat"), AdaptiveHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui || client.screen != null || !AdaptiveClient.visible()) {
			return;
		}
		int width = 72;
		int x = 8;
		int y = graphics.guiHeight() - 28;
		int fill = Math.round(width * (AdaptiveClient.phase() == FamilyPhase.READY ? 1.0F : AdaptiveClient.progress() / 100.0F));
		if (AdaptiveClient.phase() == FamilyPhase.COOLDOWN) {
			fill = Math.round(width * (1.0F - AdaptiveClient.cooldownTicks() / (float) AdaptiveClient.cooldownMax()));
		}
		int color = switch (AdaptiveClient.phase()) {
			case READY -> 0xFFE8C36A;
			case BUILDING -> 0xFF7AD7FF;
			case COOLDOWN -> 0xFF8A8A8A;
			default -> 0xFF7AD7FF;
		};
		graphics.fill(x - 1, y - 1, x + width + 1, y + 4, 0xC0101014);
		graphics.fill(x, y, x + width, y + 3, 0xFF3A3A40);
		if (fill > 0) {
			graphics.fill(x, y, x + Math.min(width, fill), y + 3, color);
		}
		Component label = AdaptiveClient.style() == null
				? Component.translatable(AdaptiveClient.family().translationKey())
				: Component.translatable(AdaptiveClient.phase() == FamilyPhase.READY ? AdaptiveClient.style().readyKey() : AdaptiveClient.style().styleKey());
		graphics.drawString(client.font, label, x, y - 10, color, true);
	}
}
