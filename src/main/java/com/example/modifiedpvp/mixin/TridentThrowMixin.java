package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ModEnchantments;
import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public class TridentThrowMixin {
	@Inject(method = "releaseUsing", at = @At("RETURN"))
	private void modifiedpvp$loosed(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfoReturnable<Boolean> callback) {
		if (entity instanceof ServerPlayer player && Boolean.TRUE.equals(callback.getReturnValue()) && ModEnchantments.level(level, Enchantments.LOYALTY, stack) > 0) {
			HybridCombatManager.onTridentLoosed(player);
		}
	}
}
