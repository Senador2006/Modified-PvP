package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.SpearRules;
import com.example.modifiedpvp.combat.WeaponStats;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpearHud {
	private static final int ICON_HEIGHT = 16;
	private static final int GAP = 2;
	private static final int HOTBAR_LEFT = 91;
	private static final int OFFHAND_OFFSET = 29;
	private static final Identifier TEXTURE = ModifiedPvP.id("textures/gui/movements/contra_golpe.png");
	private static final int TEXTURE_SIZE = 32;

	private SpearHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ModifiedPvP.id("spear_combat"), SpearHud::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker tickCounter) {
		Minecraft client = Minecraft.getInstance();
		Player player = client.player;
		if (player == null || client.options.hideGui || client.screen != null) {
			return;
		}

		int columnRight = graphics.guiWidth() / 2 - HOTBAR_LEFT - OFFHAND_OFFSET - 4;
		int stackTop = graphics.guiHeight() - (ICON_HEIGHT * 3 + GAP * 2);
		int parryY = stackTop - ICON_HEIGHT - GAP;
		int width = ICON_HEIGHT;
		int x = columnRight - width - GAP - width;
		drawIcon(graphics, x, parryY, SpearClient.cooldownReady(), SpearClient.guarding() || SpearClient.riposteReady() || SpearClient.followUpReady());

		if (SpearClient.selfBrokenTicks() > 0) {
			graphics.drawCenteredString(client.font, Component.translatable("combat.modifiedpvp.spear.broken_self"), graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 28, 0xFFE08A6A);
		}

		if (!WeaponStats.isSpear(player.getMainHandItem())) {
			return;
		}

		int targetId = SpearClient.targetId();
		if (targetId < 0) {
			return;
		}
		Entity looked = lookedEntity(client);
		if (looked == null || looked.getId() != targetId) {
			return;
		}

		int barWidth = 60;
		int barX = graphics.guiWidth() / 2 - barWidth / 2;
		int barY = graphics.guiHeight() / 2 + 14;
		int fill = Math.round(barWidth * (SpearClient.pressure() / SpearRules.PRESSURE_MAX));
		graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 4, 0xC0101014);
		graphics.fill(barX, barY, barX + barWidth, barY + 3, 0xFF3A3A40);
		if (fill > 0) {
			graphics.fill(barX, barY, barX + fill, barY + 3, SpearClient.brokenTicks() > 0 ? 0xFFE8C36A : 0xFF7AD7FF);
		}
		if (SpearClient.brokenTicks() > 0) {
			Component label = Component.translatable(SpearClient.enhanced() ? "combat.modifiedpvp.spear.broken_enhanced" : "combat.modifiedpvp.spear.broken");
			graphics.drawCenteredString(client.font, label, graphics.guiWidth() / 2, barY + 6, 0xFFE8C36A);
		} else if (SpearClient.immune()) {
			graphics.drawCenteredString(client.font, Component.translatable("combat.modifiedpvp.spear.immune"), graphics.guiWidth() / 2, barY + 6, 0xFF8A8A8A);
		}
	}

	private static void drawIcon(GuiGraphics graphics, int x, int y, float ready, boolean active) {
		graphics.fill(x - 1, y - 1, x + ICON_HEIGHT + 1, y + ICON_HEIGHT + 1, 0xC0101014);
		boolean charged = active || ready >= 1.0F;
		blit(graphics, x, y, ICON_HEIGHT, ICON_HEIGHT, 0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, charged ? 0xFFFFFFFF : 0xFF7A7A7A);
		if (!charged && ready > 0.0F) {
			int shown = Math.max(1, Math.round(ICON_HEIGHT * ready));
			int sourceShown = Math.max(1, Math.round(TEXTURE_SIZE * ready));
			blit(graphics, x, y + ICON_HEIGHT - shown, ICON_HEIGHT, shown, 0.0F, TEXTURE_SIZE - sourceShown, TEXTURE_SIZE, sourceShown, 0xFFFFFFFF);
		}
		if (active) {
			int color = SpearClient.riposteReady() || SpearClient.followUpReady() ? 0xFFE8C36A : SpearClient.perfect() ? 0xFF9BE08A : 0xFF7AD7FF;
			graphics.renderOutline(x - 1, y - 1, ICON_HEIGHT + 2, ICON_HEIGHT + 2, color);
		}
	}

	private static void blit(GuiGraphics graphics, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, regionWidth, regionHeight, TEXTURE_SIZE, TEXTURE_SIZE, color);
	}

	private static Entity lookedEntity(Minecraft client) {
		HitResult hit = client.hitResult;
		if (hit instanceof EntityHitResult entityHit) {
			return entityHit.getEntity();
		}
		return null;
	}
}
