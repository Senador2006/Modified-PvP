package com.example.modifiedpvp.combat;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final ResourceKey<Item> CRIMSON_CROWN_KEY = ResourceKey.create(Registries.ITEM, ModifiedPvP.id("crimson_crown"));
	public static final Item CRIMSON_CROWN = Registry.register(BuiltInRegistries.ITEM, CRIMSON_CROWN_KEY, new Item(new Item.Properties().setId(CRIMSON_CROWN_KEY).stacksTo(1)));

	private ModItems() {
	}

	public static void register() {
		CRIMSON_CROWN.getDescriptionId();
	}
}
