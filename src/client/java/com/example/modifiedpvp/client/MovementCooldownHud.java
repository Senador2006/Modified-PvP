package com.example.modifiedpvp.client;

import java.util.ArrayList;
import java.util.List;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.CombatRules;
import com.example.modifiedpvp.combat.CombatantAccess;
import com.example.modifiedpvp.combat.CombatantState;
import com.example.modifiedpvp.combat.WeaponStats;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class MovementCooldownHud {
	private static final int ICON_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int BELOW_CROSSHAIR = 12;

	private static final MoveIcon DASH = new MoveIcon(ModifiedPvP.id("textures/gui/movements/dashing.png"), 48, 48);
	private static final MoveIcon SLIDE = new MoveIcon(ModifiedPvP.id("textures/gui/movements/sliding.png"), 32, 10);
	private static final MoveIcon JUMP = new MoveIcon(ModifiedPvP.id("textures/gui/movements/double_jump.png"), 246, 246);
	private static final MoveIcon PARRY = new MoveIcon(ModifiedPvP.id("textures/gui/movements/parry.png"), 32, 32);
	private static final MoveIcon COUNTER = new MoveIcon(ModifiedPvP.id("textures/gui/movements/contra_golpe.png"), 31, 32);

	private MovementCooldownHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ModifiedPvP.id("movement_cooldowns"), MovementCooldownHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || client.options.hideGui || client.screen != null) {
			return;
		}

		CombatantState state = CombatantAccess.of(player);
		List<ShownIcon> icons = new ArrayList<>(5);
		if (state.parrying() || state.parryCooldown() > 0) {
			boolean parrying = state.parrying();
			icons.add(new ShownIcon(PARRY, parrying ? 1.0F : ready(state.parryCooldown(), state.parryCooldownMax()), parrying, parrying ? 0xFF7AD7FF : 0));
		}
		if (state.dashCooldown() > 0) {
			icons.add(new ShownIcon(DASH, ready(state.dashCooldown(), CombatRules.DASH_COOLDOWN), false, 0));
		}
		if (state.sliding() || state.slideCooldown() > 0) {
			boolean sliding = state.sliding();
			icons.add(new ShownIcon(SLIDE, sliding ? 1.0F : ready(state.slideCooldown(), CombatRules.SLIDE_COOLDOWN), sliding, sliding ? 0xFF7AD7FF : 0));
		}
		if (!state.doubleJumpReady()) {
			icons.add(new ShownIcon(JUMP, 1.0F, true, 0));
		}
		if (WeaponStats.isSpear(player.getMainHandItem())) {
			boolean countering = SpearClient.guarding() || SpearClient.riposteReady() || SpearClient.followUpReady();
			if (countering || SpearClient.coolingDown()) {
				int outline = 0;
				if (countering) {
					outline = SpearClient.riposteReady() || SpearClient.followUpReady() ? 0xFFE8C36A : SpearClient.perfect() ? 0xFF9BE08A : 0xFF7AD7FF;
				}
				icons.add(new ShownIcon(COUNTER, countering ? 1.0F : SpearClient.cooldownReady(), countering, outline));
			}
		}
		if (icons.isEmpty()) {
			return;
		}

		int total = 0;
		for (int i = 0; i < icons.size(); i++) {
			total += width(icons.get(i).icon());
			if (i > 0) {
				total += GAP;
			}
		}
		int x = graphics.guiWidth() / 2 - total / 2;
		int y = graphics.guiHeight() / 2 + BELOW_CROSSHAIR;
		for (ShownIcon icon : icons) {
			int iconWidth = width(icon.icon());
			draw(graphics, icon, x, y, iconWidth);
			x += iconWidth + GAP;
		}
	}

	private static float ready(int cooldown, int max) {
		if (max <= 0) {
			return 1.0F;
		}
		return 1.0F - Math.min(1.0F, cooldown / (float) max);
	}

	private static int width(MoveIcon icon) {
		return Math.max(1, Math.round(icon.textureWidth() * (ICON_HEIGHT / (float) icon.textureHeight())));
	}

	private static void draw(GuiGraphics graphics, ShownIcon shown, int x, int y, int width) {
		MoveIcon icon = shown.icon();
		graphics.fill(x - 1, y - 1, x + width + 1, y + ICON_HEIGHT + 1, 0x90101014);
		boolean charged = shown.active() || shown.ready() >= 1.0F;
		blit(graphics, icon, x, y, width, ICON_HEIGHT, 0.0F, 0.0F, icon.textureWidth(), icon.textureHeight(), charged ? 0xFFFFFFFF : 0xFF7A7A7A);
		if (!charged && shown.ready() > 0.0F) {
			int shownHeight = Math.max(1, Math.round(ICON_HEIGHT * shown.ready()));
			int sourceShown = Math.max(1, Math.round(icon.textureHeight() * shown.ready()));
			blit(graphics, icon, x, y + ICON_HEIGHT - shownHeight, width, shownHeight, 0.0F, icon.textureHeight() - sourceShown, icon.textureWidth(), sourceShown, 0xFFFFFFFF);
		}
		if (shown.outline() != 0) {
			graphics.renderOutline(x - 1, y - 1, width + 2, ICON_HEIGHT + 2, shown.outline());
		}
	}

	private static void blit(GuiGraphics graphics, MoveIcon icon, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), x, y, u, v, width, height, regionWidth, regionHeight, icon.textureWidth(), icon.textureHeight(), color);
	}

	private record MoveIcon(Identifier texture, int textureWidth, int textureHeight) {
	}

	private record ShownIcon(MoveIcon icon, float ready, boolean active, int outline) {
	}
}
