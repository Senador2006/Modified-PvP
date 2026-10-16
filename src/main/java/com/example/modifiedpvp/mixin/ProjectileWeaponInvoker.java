package com.example.modifiedpvp.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponInvoker {
	@Invoker("useAmmo")
	static ItemStack modifiedpvp$useAmmo(ItemStack weapon, ItemStack ammo, LivingEntity entity, boolean intangible) {
		throw new AssertionError();
	}
}
