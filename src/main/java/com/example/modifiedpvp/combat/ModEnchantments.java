package com.example.modifiedpvp.combat;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class ModEnchantments {
	public static final ResourceKey<Enchantment> RANGE = key("range");
	public static final ResourceKey<Enchantment> DOUBLE_HIT = key("double_hit");
	public static final ResourceKey<Enchantment> SLICE = key("slice");
	public static final ResourceKey<Enchantment> DOUBLE_ARROW = key("double_arrow");
	public static final ResourceKey<Enchantment> DRAIN = key("drain");
	public static final ResourceKey<Enchantment> PUSH = key("push");
	public static final ResourceKey<Enchantment> GRAPPLING_HOOK = key("grappling_hook");
	public static final ResourceKey<Enchantment> SHOTGUN = key("shotgun");

	private ModEnchantments() {
	}

	public static int level(Level level, ResourceKey<Enchantment> enchantment, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}
		return level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.get(enchantment)
				.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
				.orElse(0);
	}

	public static int highest(Level level, ResourceKey<Enchantment> enchantment, ItemStack first, ItemStack second) {
		return Math.max(level(level, enchantment, first), level(level, enchantment, second));
	}

	private static ResourceKey<Enchantment> key(String path) {
		return ResourceKey.create(Registries.ENCHANTMENT, ModifiedPvP.id(path));
	}
}
