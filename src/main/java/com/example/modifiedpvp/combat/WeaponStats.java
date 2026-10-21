package com.example.modifiedpvp.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;

public final class WeaponStats {
	private WeaponStats() {
	}

	public static boolean isSword(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}

	public static boolean isAxe(ItemStack stack) {
		return stack.is(ItemTags.AXES);
	}

	public static boolean isDualWielding(Player player) {
		return isSword(player.getMainHandItem()) && isSword(player.getOffhandItem());
	}

	public static boolean isShield(ItemStack stack) {
		return stack.getItem() instanceof ShieldItem;
	}

	public static boolean isSpear(ItemStack stack) {
		return stack.is(ItemTags.SPEARS);
	}

	public static boolean offhandConsumesUse(Player player) {
		ItemStack offhand = player.getOffhandItem();
		return !offhand.isEmpty() && (offhand.has(DataComponents.BLOCKS_ATTACKS)
				|| offhand.has(DataComponents.CONSUMABLE)
				|| offhand.has(DataComponents.FOOD)
				|| offhand.has(DataComponents.PIERCING_WEAPON)
				|| offhand.has(DataComponents.KINETIC_WEAPON)
				|| offhand.is(ItemTags.SPEARS));
	}

	public static float attackDamage(ItemStack stack) {
		double[] bonus = {0.0};
		stack.forEachModifier(EquipmentSlotGroup.MAINHAND, (attribute, modifier, display) -> {
			if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				bonus[0] += modifier.amount();
			}
		});
		return 1.0F + (float) bonus[0];
	}

	public static Vec3 horizontalLook(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		if (flat.lengthSqr() < 1.0E-4) {
			double yaw = Math.toRadians(player.getYRot());
			flat = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		return flat.normalize();
	}

	public static boolean canUseMovement(Player player) {
		return player.isAlive()
				&& !player.isSpectator()
				&& !ModEffects.isStunned(player)
				&& !player.isPassenger()
				&& !player.getAbilities().flying
				&& !player.isFallFlying();
	}
}
