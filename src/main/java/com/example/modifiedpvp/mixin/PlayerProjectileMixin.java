package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.BowFeatures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerProjectileMixin {
	@Inject(method = "getProjectile", at = @At("HEAD"), cancellable = true)
	private void modifiedpvp$bundleArrow(ItemStack weapon, CallbackInfoReturnable<ItemStack> callback) {
		if (!(weapon.getItem() instanceof BowItem)) {
			return;
		}
		ItemStack arrow = BowFeatures.bundleArrow((Player) (Object) this);
		if (!arrow.isEmpty()) {
			callback.setReturnValue(arrow);
		}
	}
}
