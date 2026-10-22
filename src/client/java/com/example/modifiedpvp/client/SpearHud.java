package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.SpearRules;
import com.example.modifiedpvp.combat.WeaponStats;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpearHud {
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

		int feedbackY = graphics.guiHeight() / 2 + 36;
		if (SpearClient.selfBrokenTicks() > 0 && !lookingAtPressureTarget(client)) {
			graphics.drawCenteredString(client.font, Component.translatable("combat.modifiedpvp.spear.broken_self"), graphics.guiWidth() / 2, feedbackY, 0xFFE08A6A);
		}

		if (!WeaponStats.isSpear(player.getMainHandItem()) || !lookingAtPressureTarget(client)) {
			return;
		}

		int barWidth = 60;
		int barX = graphics.guiWidth() / 2 - barWidth / 2;
		int fill = Math.round(barWidth * (SpearClient.pressure() / SpearRules.PRESSURE_MAX));
		graphics.fill(barX - 1, feedbackY - 1, barX + barWidth + 1, feedbackY + 4, 0xC0101014);
		graphics.fill(barX, feedbackY, barX + barWidth, feedbackY + 3, 0xFF3A3A40);
		if (fill > 0) {
			graphics.fill(barX, feedbackY, barX + fill, feedbackY + 3, SpearClient.brokenTicks() > 0 ? 0xFFE8C36A : 0xFF7AD7FF);
		}
		int labelY = feedbackY + 6;
		if (SpearClient.brokenTicks() > 0) {
			Component label = Component.translatable(SpearClient.enhanced() ? "combat.modifiedpvp.spear.broken_enhanced" : "combat.modifiedpvp.spear.broken");
			graphics.drawCenteredString(client.font, label, graphics.guiWidth() / 2, labelY, 0xFFE8C36A);
			labelY += 10;
		} else if (SpearClient.immune()) {
			graphics.drawCenteredString(client.font, Component.translatable("combat.modifiedpvp.spear.immune"), graphics.guiWidth() / 2, labelY, 0xFF8A8A8A);
			labelY += 10;
		}
		if (SpearClient.selfBrokenTicks() > 0) {
			graphics.drawCenteredString(client.font, Component.translatable("combat.modifiedpvp.spear.broken_self"), graphics.guiWidth() / 2, labelY, 0xFFE08A6A);
		}
	}

	private static boolean lookingAtPressureTarget(Minecraft client) {
		int targetId = SpearClient.targetId();
		if (targetId < 0) {
			return false;
		}
		HitResult hit = client.hitResult;
		return hit instanceof EntityHitResult entityHit && entityHit.getEntity().getId() == targetId;
	}
}
