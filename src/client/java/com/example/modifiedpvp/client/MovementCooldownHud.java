package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class MovementCooldownHud {
	private static final int ICON_HEIGHT = 16;
	private static final int GAP = 2;
	private static final int HOTBAR_LEFT = 91;
	private static final int OFFHAND_OFFSET = 29;

	private static final MoveIcon DASH = new MoveIcon(ModifiedPvP.id("textures/gui/movements/dashing.png"), 36, 32);
	private static final MoveIcon SLIDE = new MoveIcon(ModifiedPvP.id("textures/gui/movements/sliding.png"), 55, 32);
	private static final MoveIcon JUMP = new MoveIcon(ModifiedPvP.id("textures/gui/movements/double_jump.png"), 17, 31);
	private static final MoveIcon PARRY = new MoveIcon(ModifiedPvP.id("textures/gui/movements/parry.png"), 32, 32);

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
		int right = graphics.guiWidth() / 2 - HOTBAR_LEFT - OFFHAND_OFFSET - 4;
		int y = graphics.guiHeight() - (ICON_HEIGHT * 3 + GAP * 2);

		draw(graphics, PARRY, right, y - ICON_HEIGHT - GAP, state.parrying() ? 1.0F : ready(state.parryCooldown(), state.parryCooldownMax()), state.parrying());
		draw(graphics, DASH, right, y, ready(state.dashCooldown(), CombatRules.DASH_COOLDOWN), false);
		draw(graphics, SLIDE, right, y + ICON_HEIGHT + GAP, state.sliding() ? 1.0F : ready(state.slideCooldown(), CombatRules.SLIDE_COOLDOWN), state.sliding());
		draw(graphics, JUMP, right, y + (ICON_HEIGHT + GAP) * 2, state.doubleJumpReady() ? 1.0F : 0.0F, false);
	}

	private static float ready(int cooldown, int max) {
		if (max <= 0) {
			return 1.0F;
		}
		return 1.0F - Math.min(1.0F, cooldown / (float) max);
	}

	private static void draw(GuiGraphics graphics, MoveIcon icon, int right, int y, float ready, boolean active) {
		int width = Math.max(1, Math.round(icon.textureWidth() * (ICON_HEIGHT / (float) icon.textureHeight())));
		int x = right - width;
		graphics.fill(x - 1, y - 1, x + width + 1, y + ICON_HEIGHT + 1, 0xC0101014);

		boolean charged = active || ready >= 1.0F;
		blit(graphics, icon, x, y, width, ICON_HEIGHT, 0.0F, 0.0F, icon.textureWidth(), icon.textureHeight(), charged ? 0xFFFFFFFF : 0xFF7A7A7A);
		if (!charged && ready > 0.0F) {
			int shown = Math.max(1, Math.round(ICON_HEIGHT * ready));
			int sourceShown = Math.max(1, Math.round(icon.textureHeight() * ready));
			blit(graphics, icon, x, y + ICON_HEIGHT - shown, width, shown, 0.0F, icon.textureHeight() - sourceShown, icon.textureWidth(), sourceShown, 0xFFFFFFFF);
		}
		if (active) {
			graphics.renderOutline(x - 1, y - 1, width + 2, ICON_HEIGHT + 2, 0xFF7AD7FF);
		}
	}

	private static void blit(GuiGraphics graphics, MoveIcon icon, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, u, v, width, height, regionWidth, regionHeight, icon.textureWidth(), icon.textureHeight(), color);
	}

	private record MoveIcon(Identifier texture, int textureWidth, int textureHeight) {
	}
}
