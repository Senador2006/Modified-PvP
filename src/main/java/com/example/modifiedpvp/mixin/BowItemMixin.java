package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ArrowVolleyAccess;
import com.example.modifiedpvp.combat.BowFeatures;
import com.example.modifiedpvp.combat.ComboSpecials;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {
	@Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private float modifiedpvp$offhandDraw(int ticks, ItemStack stack, Level level, LivingEntity entity, int remaining) {
		if (entity instanceof Player player && ComboSpecials.rapidBow(player)) {
			return 1.0F;
		}
		return BowItem.getPowerForTime(BowFeatures.scaledDrawTicks(entity, ticks));
	}

	@Inject(method = "shootProjectile", at = @At("HEAD"))
	private void modifiedpvp$markCompanion(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, LivingEntity target, CallbackInfo callback) {
		if (index > 0 && projectile instanceof ArrowVolleyAccess access) {
			access.modifiedpvp$setVolleyCompanion(true);
		}
	}

	@Inject(method = "releaseUsing", at = @At("RETURN"))
	private void modifiedpvp$compactBundle(ItemStack stack, Level level, LivingEntity entity, int remaining, CallbackInfoReturnable<Boolean> callback) {
		if (callback.getReturnValueZ() && !level.isClientSide() && entity instanceof Player player) {
			BowFeatures.compactBundle(player);
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && ComboSpecials.rapidBow(serverPlayer)) {
				ComboSpecials.consumeBowShot(serverPlayer);
			}
		}
	}
}
