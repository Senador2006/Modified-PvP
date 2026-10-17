package com.example.modifiedpvp.loot;

import java.util.Map;

import com.example.modifiedpvp.ModifiedPvP;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public final class ModLoot {
	private static final ResourceKey<LootTable> OMINOUS_VAULT_RARE = vanilla("chests/trial_chambers/reward_ominous_rare");
	private static final ResourceKey<LootTable> OMINOUS_VAULT_CHAIN = inject("ominous_vault_chain");
	private static final int OMINOUS_VAULT_CHAIN_WEIGHT = 2;

	private static final Map<ResourceKey<LootTable>, ResourceKey<LootTable>> EXTRA_POOLS = Map.of(
			vanilla("chests/stronghold_library"), inject("stronghold_library"),
			vanilla("chests/pillager_outpost"), inject("pillager_outpost"),
			vanilla("chests/underwater_ruin_big"), inject("underwater_ruin_big"),
			vanilla("chests/underwater_ruin_small"), inject("underwater_ruin_small"));

	private ModLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			ResourceKey<LootTable> extra = EXTRA_POOLS.get(key);
			if (extra != null) {
				tableBuilder.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1.0F))
						.add(NestedLootTable.lootTableReference(extra)));
			}
			if (key.equals(OMINOUS_VAULT_RARE)) {
				tableBuilder.modifyPools(pool -> pool.add(NestedLootTable.lootTableReference(OMINOUS_VAULT_CHAIN).setWeight(OMINOUS_VAULT_CHAIN_WEIGHT)));
			}
		});
	}

	private static ResourceKey<LootTable> vanilla(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(path));
	}

	private static ResourceKey<LootTable> inject(String path) {
		return ResourceKey.create(Registries.LOOT_TABLE, ModifiedPvP.id("inject/" + path));
	}
}
