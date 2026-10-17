package com.example.modifiedpvp.mixin;

import com.example.modifiedpvp.combat.CombatRules;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.Items;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Drowned.class)
public abstract class DrownedTridentMixin {
	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void modifiedpvp$tridentDropChance(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
		Drowned drowned = (Drowned) (Object) this;
		if (drowned.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.TRIDENT)) {
			drowned.setDropChance(EquipmentSlot.MAINHAND, CombatRules.DROWNED_TRIDENT_DROP_CHANCE);
		}
	}
}
