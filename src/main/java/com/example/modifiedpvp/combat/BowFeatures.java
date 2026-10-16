package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public final class BowFeatures {
	private static final int NO_SELECTED_ITEM = -1;

	private BowFeatures() {
	}

	public static boolean holdingBow(Player player) {
		return player.getMainHandItem().getItem() instanceof BowItem || player.getOffhandItem().getItem() instanceof BowItem;
	}

	public static boolean hasOffhandArrows(LivingEntity entity) {
		ItemStack offhand = entity.getOffhandItem();
		if (offhand.is(ItemTags.ARROWS)) {
			return true;
		}
		return isArrowBundle(offhand);
	}

	public static int scaledDrawTicks(LivingEntity entity, int ticks) {
		if (!hasOffhandArrows(entity)) {
			return ticks;
		}
		return Math.round(ticks * CombatRules.OFFHAND_DRAW_SPEED);
	}

	public static ItemStack bundleArrow(Player player) {
		if (player.getOffhandItem().is(ItemTags.ARROWS)) {
			return ItemStack.EMPTY;
		}
		ItemStack bundle = findArrowBundle(player);
		if (bundle.isEmpty()) {
			return ItemStack.EMPTY;
		}
		BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return ItemStack.EMPTY;
		}
		int selected = contents.getSelectedItem();
		if (selected >= 0 && selected < contents.size()) {
			ItemStack chosen = contents.getItemUnsafe(selected);
			if (chosen.is(ItemTags.ARROWS)) {
				return chosen;
			}
		}
		for (int i = 0; i < contents.size(); i++) {
			ItemStack stack = contents.getItemUnsafe(i);
			if (stack.is(ItemTags.ARROWS)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static boolean cycleBundleArrow(Player player) {
		if (!holdingBow(player)) {
			return false;
		}
		ItemStack bundle = findArrowBundle(player);
		BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		List<Integer> arrows = arrowIndices(contents);
		if (arrows.isEmpty()) {
			return false;
		}
		int current = arrows.indexOf(contents.getSelectedItem());
		int next = arrows.get((current + 1) % arrows.size());
		if (contents.getSelectedItem() != next) {
			BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
			mutable.toggleSelectedItem(next);
			bundle.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
			markInventory(player);
		}
		ItemStack arrow = contents.getItemUnsafe(next);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(Component.translatable("combat.modifiedpvp.bundle_arrow", arrow.getHoverName()), true);
			serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 0.7F, 1.15F);
		}
		return true;
	}

	public static void compactBundle(Player player) {
		ItemStack bundle = findBundleToCompact(player);
		if (bundle.isEmpty()) {
			return;
		}
		BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return;
		}
		List<ItemStack> kept = new ArrayList<>();
		int selected = contents.getSelectedItem();
		int mapped = NO_SELECTED_ITEM;
		for (int i = 0; i < contents.size(); i++) {
			ItemStack stack = contents.getItemUnsafe(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (i == selected) {
				mapped = kept.size();
			}
			kept.add(stack);
		}
		if (mapped == NO_SELECTED_ITEM) {
			for (int i = 0; i < kept.size(); i++) {
				if (kept.get(i).is(ItemTags.ARROWS)) {
					mapped = i;
					break;
				}
			}
		}
		BundleContents.Mutable mutable = new BundleContents.Mutable(new BundleContents(List.copyOf(kept)));
		if (mapped >= 0) {
			mutable.toggleSelectedItem(mapped);
		}
		bundle.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
		markInventory(player);
	}

	private static ItemStack findBundleToCompact(Player player) {
		ItemStack emptied = findBundle(player, BowFeatures::hasEmptyStack);
		if (!emptied.isEmpty()) {
			return emptied;
		}
		return findArrowBundle(player);
	}

	private static ItemStack findArrowBundle(Player player) {
		return findBundle(player, BowFeatures::isArrowBundle);
	}

	private static ItemStack findBundle(Player player, java.util.function.Predicate<ItemStack> predicate) {
		ItemStack offhand = player.getOffhandItem();
		if (predicate.test(offhand)) {
			return offhand;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (predicate.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static boolean isArrowBundle(ItemStack stack) {
		if (!stack.is(ItemTags.BUNDLES)) {
			return false;
		}
		BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		for (int i = 0; i < contents.size(); i++) {
			if (contents.getItemUnsafe(i).is(ItemTags.ARROWS)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasEmptyStack(ItemStack stack) {
		if (!stack.is(ItemTags.BUNDLES)) {
			return false;
		}
		BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
		if (contents == null) {
			return false;
		}
		for (int i = 0; i < contents.size(); i++) {
			if (contents.getItemUnsafe(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static List<Integer> arrowIndices(BundleContents contents) {
		List<Integer> arrows = new ArrayList<>();
		for (int i = 0; i < contents.size(); i++) {
			if (contents.getItemUnsafe(i).is(ItemTags.ARROWS)) {
				arrows.add(i);
			}
		}
		return arrows;
	}

	private static void markInventory(Player player) {
		player.getInventory().setChanged();
		player.inventoryMenu.broadcastChanges();
	}
}
