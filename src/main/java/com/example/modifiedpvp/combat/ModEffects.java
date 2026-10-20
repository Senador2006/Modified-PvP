package com.example.modifiedpvp.combat;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

public final class ModEffects {
	public static final Holder<MobEffect> STUN = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ModifiedPvP.id("stun"), new StunEffect()
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, ModifiedPvP.id("effect.stun"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, ModifiedPvP.id("effect.stun"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

	private ModEffects() {
	}

	public static void register() {
	}

	public static boolean isStunned(LivingEntity entity) {
		return entity.hasEffect(STUN);
	}

	private static final class StunEffect extends MobEffect {
		private StunEffect() {
			super(MobEffectCategory.HARMFUL, 0xF2D64B);
		}

		@Override
		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
			return true;
		}

		@Override
		public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
			entity.setSprinting(false);
			entity.setDeltaMovement(0.0, Math.min(entity.getDeltaMovement().y, 0.0), 0.0);
			if (entity instanceof Mob mob) {
				mob.setTarget(null);
				mob.setAggressive(false);
				mob.getNavigation().stop();
				mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
			}
			if (entity instanceof Player player && player.isUsingItem()) {
				player.stopUsingItem();
			}
			return true;
		}
	}
}
