package com.example.modifiedpvp.combat;

public final class CombatRules {
	public static final int DASH_COOLDOWN = 16;
	public static final double DASH_SPEED = 0.92;
	public static final double DASH_SPEED_PER_LEVEL = 0.42;

	public static final double DOUBLE_JUMP_MULTIPLIER = 0.92;
	public static final double JUMPING_PER_LEVEL = 0.30;

	public static final double KICK_LIFT = 0.46;
	public static final double KICK_LIFT_PER_LEVEL = 0.26;
	public static final double KICK_SHOVE = 0.18;
	public static final double KICK_SHOVE_PER_LEVEL = 0.06;

	public static final int SLIDE_COOLDOWN = 24;
	public static final int SLIDE_MAX_TICKS = 16;
	public static final double SLIDE_SPEED = 0.52;

	public static final int THRUST_COOLDOWN = 10;
	public static final float THRUST_MIN_STRENGTH = 0.85F;
	public static final double THRUST_BASE_RANGE = 3.5;
	public static final double THRUST_RANGE_PER_LEVEL = 0.8;
	public static final double THRUST_LUNGE = 0.58;
	public static final double THRUST_CLOSE_RANGE = 1.6;

	public static final int PARRY_WINDOW = 8;
	public static final int PARRY_HITSTOP_TICKS = 10;
	public static final int PARRY_SUCCESS_COOLDOWN = 60 * 20;
	public static final int PARRY_MISS_COOLDOWN = 30 * 20;
	public static final float PARRY_REFLECT_MULTIPLIER = 2.0F;
	public static final int PARRY_STUN_TICKS = 3 * 20;

	public static final int HEAVY_MIN_TICKS = 8;
	public static final int HEAVY_FULL_TICKS = 24;
	public static final int HEAVY_COOLDOWN = 28;
	public static final double HEAVY_RANGE = 3.3;

	public static final float DUAL_WIELD_MIN_STRENGTH = 0.8F;
	public static final float DUAL_WIELD_OFFHAND_RATIO = 0.68F;

	public static final float OFFHAND_DRAW_SPEED = 1.6F;

	public static final int LOYALTY_PULL_BASE_TICKS = 12;
	public static final int LOYALTY_PULL_TICKS_PER_LEVEL = 6;
	public static final int LOYALTY_STUN_BASE_TICKS = 16;
	public static final int LOYALTY_STUN_TICKS_PER_LEVEL = 8;
	public static final double LOYALTY_PULL_SPEED = 0.28;
	public static final double LOYALTY_PULL_SPEED_PER_LEVEL = 0.12;
	public static final float DROWNED_TRIDENT_DROP_CHANCE = 0.7F;

	public static final int SHIELD_PUSH_COOLDOWN = 10;
	public static final double SHIELD_PUSH_RANGE = 2.75;
	public static final double SHIELD_PUSH_STRENGTH = 0.5;
	public static final double SHIELD_PUSH_PER_LEVEL = 0.28;

	public static final int SHOTGUN_BASE_PROJECTILES = 2;
	public static final double GRAPPLE_SPEED = 1.05;
	public static final int GRAPPLE_MAX_TICKS = 40;

	public static final double MACE_CHAIN_BASE_RANGE = 10.0;
	public static final double MACE_CHAIN_RANGE_PER_LEVEL = 4.0;
	public static final double MACE_CHAIN_THROW_SPEED = 1.8;
	public static final double MACE_CHAIN_RETURN_SPEED = 2.2;
	public static final double MACE_CHAIN_GRAVITY = 0.025;
	public static final int MACE_CHAIN_MAX_TICKS = 100;
	public static final int MACE_CHAIN_COOLDOWN = 30;
	public static final float MACE_CHAIN_DAMAGE_RATIO = 0.5F;
	public static final double MACE_SHOCKWAVE_RADIUS = 3.5;
	public static final double MACE_SHOCKWAVE_STRENGTH = 1.7;
	public static final double MACE_SHOCKWAVE_LIFT = 0.72;
	public static final double MACE_SHOCKWAVE_SELF_STRENGTH = 1.65;

	public static final float COMBO_MIN_STRENGTH = 0.9F;
	public static final int COMBO_WINDOW_SWORD = 40;
	public static final int COMBO_WINDOW_DUAL = 40;
	public static final int COMBO_WINDOW_AXE = 100;
	public static final int COMBO_WINDOW_BOW = 80;
	public static final int COMBO_WINDOW_TRIDENT = 240;

	public static final int SWORD_COMBO_HITS = 5;
	public static final int SWORD_BUFF_TICKS = 15 * 20;
	public static final int SWORD_COMBO_COOLDOWN = 60 * 20;
	public static final int SWORD_KILL_GOAL = 50;

	public static final int DUAL_COMBO_HITS = 7;
	public static final int DUAL_BUFF_TICKS = 90 * 20;
	public static final int DUAL_COMBO_COOLDOWN = 120 * 20;
	public static final int DUAL_KILL_GOAL = 50;

	public static final int AXE_COMBO_HITS = 3;
	public static final int AXE_STUN_TICKS = 3 * 20;
	public static final int AXE_COMBO_COOLDOWN = 50 * 20;
	public static final float AXE_EMPOWER_DAMAGE = 2.0F;
	public static final int HEAVY_GOAL = 20;

	public static final int BOW_COMBO_HITS = 7;
	public static final int BOW_BUFF_TICKS = 10 * 20;
	public static final int BOW_COMBO_COOLDOWN = 30 * 20;
	public static final int BOW_MOB_GOAL = 10;

	public static final int TRIDENT_COMBO_HITS = 5;
	public static final int TRIDENT_SLOW_TICKS = 20 * 20;
	public static final int TRIDENT_SLOW_AMPLIFIER = 2;
	public static final int TRIDENT_COMBO_COOLDOWN = 60 * 20;

	public static final int COMBO_WINDOW_SHIELD = 100;
	public static final int SHIELD_COMBO_BLOCKS = 3;
	public static final int SHIELD_COMBO_COOLDOWN = 45 * 20;
	public static final int SHIELD_BLOCK_GOAL = 30;
	public static final float SHIELD_REFLECT_RATIO = 0.5F;
	public static final double SHIELD_REFLECT_LIFT = 0.72;

	public static final int COMBO_WINDOW_MACE = 160;
	public static final int MACE_COMBO_HITS = 3;
	public static final int MACE_COMBO_COOLDOWN = 70 * 20;
	public static final int MACE_CHAIN_GOAL = 10;
	public static final int MACE_SLOW_TICKS = 4 * 20;
	public static final int MACE_SLOW_AMPLIFIER = 1;
	public static final double MACE_PULL_STRENGTH = 0.65;

	public static final int PARRY_PERFECT_TICKS = 4;
	public static final int PARRY_INTERRUPT_TICKS = 20;
	public static final float PARRY_EXPLOSION_MITIGATION = 0.5F;
	public static final float PARRY_LATE_MITIGATION = 0.45F;
	public static final double PARRY_MELEE_SHOVE = 0.35;

	public static final int CONTROL_RESIST_TICKS = 40;
	public static final int DASH_COOLDOWN_REDUCTION = 6;
	public static final int HEAVY_RECOVERY_TICKS = 12;

	public static final int SWORD_EXECUTE_TICKS = 40;
	public static final int DUAL_FLURRY_TICKS = 80;
	public static final int DUAL_EXPOSE_TICKS = 16;
	public static final int BOW_PRECISION_SHOTS = 3;
	public static final int AXE_EXPOSE_TICKS = 30;
	public static final int TRIDENT_PVP_EXPOSE_TICKS = 50;

	public static final float SHOTGUN_PLAYER_FALLOFF = 0.5F;
	public static final float DOUBLE_ARROW_PVP_RATIO = 0.4F;
	public static final double MACE_PVP_SELF_STRENGTH = 1.25;

	private CombatRules() {
	}

	public static int dashCooldown(int dashing) {
		return Math.max(8, DASH_COOLDOWN - dashing * DASH_COOLDOWN_REDUCTION);
	}
}
