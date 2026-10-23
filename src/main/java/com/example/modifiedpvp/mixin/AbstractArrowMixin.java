package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.ArrowVolleyAccess;
import com.example.modifiedpvp.combat.ComboSpecials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public class AbstractArrowMixin implements ArrowVolleyAccess {
	@Unique
	private boolean modifiedpvp$volleyCompanion;

	@Override
	public void modifiedpvp$setVolleyCompanion(boolean companion) {
		this.modifiedpvp$volleyCompanion = companion;
	}

	@Override
	public boolean modifiedpvp$isVolleyCompanion() {
		return this.modifiedpvp$volleyCompanion;
	}

	@Inject(method = "onHitEntity", at = @At("RETURN"))
	private void modifiedpvp$bowCombo(EntityHitResult hit, CallbackInfo callback) {
		if (this.modifiedpvp$volleyCompanion) {
			return;
		}
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (arrow instanceof ThrownTrident || arrow.level().isClientSide() || !(arrow.getOwner() instanceof ServerPlayer player)) {
			return;
		}
		ItemStack weapon = arrow.getWeaponItem();
		if (weapon == null || !(weapon.getItem() instanceof BowItem) || !(hit.getEntity() instanceof LivingEntity living) || living == player) {
			return;
		}
		if (living.getLastHurtByMob() != player || living.getLastHurtByMobTimestamp() != living.tickCount) {
			return;
		}
		ComboSpecials.onBowHit(player, living);
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void modifiedpvp$cartIgnition(EntityHitResult hit, CallbackInfo callback) {
		AbstractArrow arrow = (AbstractArrow) (Object) this;
		if (arrow instanceof ThrownTrident || arrow.level().isClientSide() || !(arrow.getOwner() instanceof ServerPlayer player)) {
			return;
		}
		Entity entity = hit.getEntity();
		if (entity == null || entity.getType() != EntityType.TNT_MINECART) {
			return;
		}
		ComboSpecials.onBowCartIgnition(player);
	}

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void modifiedpvp$companionIgnoresInvulnerability(EntityHitResult hit, CallbackInfo callback) {
		if (!this.modifiedpvp$volleyCompanion) {
			return;
		}
		Entity entity = hit.getEntity();
		if (entity != null) {
			entity.invulnerableTime = 0;
		}
	}
}
