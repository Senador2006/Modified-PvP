package com.example.modifiedpvp.client;

import java.util.Locale;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class MovementCooldownHud {
	private static final int BAR_WIDTH = 42;
	private static final int BAR_HEIGHT = 4;
	private static final int GAP = 8;

	private MovementCooldownHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ModifiedPvP.id("movement_cooldowns"), MovementCooldownHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.options.hideGui || client.screen != null) {
			return;
		}

		CombatantState state = CombatantAccess.of(client.player);
		Font font = client.font;
		int totalWidth = BAR_WIDTH * 3 + GAP * 2;
		int x = (graphics.guiWidth() - totalWidth) / 2;
		int y = graphics.guiHeight() - 64;

		draw(graphics, font, x, y, Component.translatable("combat.modifiedpvp.cooldown.dash"), ready(state.dashCooldown(), CombatRules.DASH_COOLDOWN), false, state.dashCooldown());
		draw(graphics, font, x + BAR_WIDTH + GAP, y, Component.translatable("combat.modifiedpvp.cooldown.slide"), state.sliding() ? 1.0F : ready(state.slideCooldown(), CombatRules.SLIDE_COOLDOWN), state.sliding(), state.sliding() ? 0 : state.slideCooldown());
		draw(graphics, font, x + (BAR_WIDTH + GAP) * 2, y, Component.translatable("combat.modifiedpvp.cooldown.jump"), state.doubleJumpReady() ? 1.0F : 0.0F, false, -1);
	}

	private static float ready(int cooldown, int max) {
		if (max <= 0) {
			return 1.0F;
		}
		return 1.0F - Math.min(1.0F, cooldown / (float) max);
	}

	private static void draw(GuiGraphics graphics, Font font, int x, int y, Component name, float ready, boolean active, int cooldownTicks) {
		String label = name.getString();
		if (!active && ready < 1.0F && cooldownTicks > 0) {
			label = label + " " + String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0F);
		}
		int textColor = active ? 0xFF7AD7FF : ready >= 1.0F ? 0xFF7CFF9A : 0xFFE8E8E8;
		graphics.drawString(font, label, x, y, textColor, true);

		int barY = y + 10;
		graphics.fill(x, barY, x + BAR_WIDTH, barY + BAR_HEIGHT, 0xC0101014);
		int fill = Math.round(BAR_WIDTH * Math.min(1.0F, Math.max(0.0F, ready)));
		if (fill <= 0) {
			return;
		}
		int color = active ? 0xFF5EC8FF : ready >= 1.0F ? 0xFF3DDC7A : 0xFFE0B15A;
		graphics.fill(x, barY, x + fill, barY + BAR_HEIGHT, color);
	}
}
