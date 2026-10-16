package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CrossbowFeatures;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
	@Inject(method = "tryLoadProjectiles", at = @At("HEAD"), cancellable = true)
	private static void modifiedpvp$shotgunLoad(LivingEntity entity, ItemStack weapon, CallbackInfoReturnable<Boolean> callback) {
		if (CrossbowFeatures.tryLoadShotgun(entity, weapon)) {
			callback.setReturnValue(!weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).isEmpty());
		}
	}

	@Inject(method = "performShooting", at = @At("HEAD"))
	private void modifiedpvp$beginShot(Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, float power, float inaccuracy, LivingEntity target, CallbackInfo callback) {
		CrossbowFeatures.beginShot(weapon);
	}

	@Inject(method = "performShooting", at = @At("RETURN"))
	private void modifiedpvp$endShot(Level level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, float power, float inaccuracy, LivingEntity target, CallbackInfo callback) {
		CrossbowFeatures.endShot();
	}

	@Inject(method = "shootProjectile", at = @At("TAIL"))
	private void modifiedpvp$markGrapple(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, LivingEntity target, CallbackInfo callback) {
		CrossbowFeatures.markProjectile(projectile);
	}
}
