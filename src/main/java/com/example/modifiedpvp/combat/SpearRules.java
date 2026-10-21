package com.example.modifiedpvp.combat;

/**
 * Constantes da Spear 1.2.0. Ajuste aqui; o servidor é a autoridade.
 * Contra Golpe ainda não cobre projéteis — só melee frontal com atacante.
 */
public final class SpearRules {
	public static final int GUARD_TOTAL_TICKS = 12;
	public static final int GUARD_PERFECT_TICKS = 4;
	public static final int GUARD_COOLDOWN_TICKS = 120;
	public static final double GUARD_CONE_DEGREES = 120.0;
	public static final float GUARD_DAMAGE_REDUCTION = 0.60F;
	public static final int RIPOSTE_WINDOW_TICKS = 8;
	public static final int PERFECT_THRUSTS = 2;
	public static final float RIPOSTE_THRUST_MULTIPLIER = 1.0F;

	public static final float PRESSURE_MAX = 100.0F;
	public static final float PRESSURE_JAB = 8.0F;
	public static final float PRESSURE_JAB_EFFECTIVE = 12.0F;
	public static final float PRESSURE_CHARGE = 18.0F;
	public static final float PRESSURE_SHIELD = 20.0F;
	public static final float PRESSURE_RIPOSTE = 35.0F;
	public static final float PRESSURE_ROMPER_RIPOSTE = 15.0F;
	public static final float PRESSURE_ROMPER_FOLLOW_UP = 15.0F;
	public static final float JAB_MIN_STRENGTH = 0.85F;
	public static final double JAB_EFFECTIVE_DEPTH = 1.5;
	public static final int CHARGE_REPEAT_TICKS = 10;
	public static final int PRESSURE_DECAY_DELAY_TICKS = 60;
	public static final float PRESSURE_DECAY_PER_SECOND = 5.0F;

	public static final int BREAK_PVP_TICKS = 16;
	public static final int BREAK_PVP_IMMUNITY_TICKS = 160;
	public static final int BREAK_MOB_TICKS = 30;
	public static final int BREAK_MOB_IMMUNITY_TICKS = 80;
	public static final int BREAK_BOSS_TICKS = 16;
	public static final int BREAK_BOSS_IMMUNITY_TICKS = 200;
	public static final int BREAK_PVP_SLOW_AMPLIFIER = 1;

	public static final int ROMPER_FOLLOW_UP_TICKS = 20;

	public static final int FINISHER_COOLDOWN_TICKS = 100;
	public static final float FINISHER_MULTIPLIER = 3.0F;
	public static final float FINISHER_ENHANCED_MULTIPLIER = 3.5F;
	public static final float FINISHER_BOSS_MULTIPLIER = 2.0F;
	public static final float FINISHER_PVP_CAP = 18.0F;
	public static final float FINISHER_ENHANCED_PVP_CAP = 22.0F;
	public static final float FINISHER_PVP_MAX_HEALTH_RATIO = 0.55F;
	public static final double FINISHER_REACH_BONUS = 0.5;
	public static final double FINISHER_MIN_DOT = 0.75;
	public static final double FINISHER_KNOCKBACK = 0.5;
	public static final double ENHANCED_WAVE_RADIUS = 3.0;
	public static final double ENHANCED_WAVE_STRENGTH = 0.6;
	public static final int ENHANCED_WAVE_SLOW_TICKS = 20;

	public static final float CIRCULAR_MULTIPLIER = 2.0F;
	public static final float CIRCULAR_ENHANCED_MULTIPLIER = 2.4F;
	public static final float CIRCULAR_BOSS_MULTIPLIER = 1.4F;
	public static final float CIRCULAR_PVP_CAP = 14.0F;
	public static final float CIRCULAR_ENHANCED_PVP_CAP = 16.0F;
	public static final float CIRCULAR_SECONDARY_MOB_RATIO = 0.55F;
	public static final float CIRCULAR_SECONDARY_PLAYER_RATIO = 0.20F;
	public static final float CIRCULAR_SECONDARY_PVP_CAP = 4.0F;
	public static final double CIRCULAR_RADIUS = 3.5;
	public static final double CIRCULAR_KNOCKBACK = 0.85;
	public static final double CIRCULAR_SECONDARY_KNOCKBACK = 0.55;
	public static final int CIRCULAR_SPIN_TICKS = 6;

	public static final int PERFECT_COUNTER_GOAL = 10;

	private SpearRules() {
	}
}
