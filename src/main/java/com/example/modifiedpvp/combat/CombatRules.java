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
	public static final double MACE_SHOCKWAVE_STRENGTH = 1.1;
	public static final double MACE_SHOCKWAVE_LIFT = 0.42;
	public static final double MACE_SHOCKWAVE_SELF_STRENGTH = 1.2;

	private CombatRules() {
	}
}
