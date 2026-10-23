package com.example.modifiedpvp.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CombatantState {
	private int dashCooldown;
	private int dashCooldownMax = CombatRules.DASH_COOLDOWN;
	private int thrustCooldown;
	private int heavyCooldown;
	private int heavyRecovery;
	private int slideCooldown;
	private boolean doubleJumpReady = true;
	private int groundTicks;
	private boolean sliding;
	private int slideTicks;
	private double slideX;
	private double slideZ;
	private final Set<UUID> slideHits = new HashSet<>();
	private boolean heavyCharging;
	private long heavyStartedAt;
	private int parryWindow;
	private int parryCooldown;
	private int parryCooldownMax = CombatRules.PARRY_MISS_COOLDOWN;
	private boolean parrySucceeded;
	private boolean parryResolved;
	private long shieldRaisedAt = Long.MIN_VALUE;
	private boolean shieldRaised;

	public void tick(Player player) {
		if (this.parryCooldown > 0) {
			this.parryCooldown--;
		}
		if (this.parryWindow > 0 && --this.parryWindow == 0) {
			this.setParryCooldown(this.parrySucceeded ? CombatRules.PARRY_SUCCESS_COOLDOWN : CombatRules.PARRY_MISS_COOLDOWN);
			this.parryResolved = true;
		}
		if (this.dashCooldown > 0) {
			this.dashCooldown--;
		}
		if (this.thrustCooldown > 0) {
			this.thrustCooldown--;
		}
		if (this.heavyCooldown > 0) {
			this.heavyCooldown--;
		}
		if (this.heavyRecovery > 0) {
			this.heavyRecovery--;
		}
		if (this.slideCooldown > 0) {
			this.slideCooldown--;
		}

		if (player.onGround()) {
			this.groundTicks++;
			if (this.groundTicks >= 2) {
				this.doubleJumpReady = true;
			}
		} else {
			this.groundTicks = 0;
		}

		boolean blocking = player.isUsingItem() && WeaponStats.isShield(player.getUseItem());
		if (blocking && !this.shieldRaised) {
			this.shieldRaised = true;
			this.shieldRaisedAt = player.level().getGameTime();
		} else if (!blocking) {
			this.shieldRaised = false;
			this.shieldRaisedAt = Long.MIN_VALUE;
		}

		if (!this.sliding) {
			return;
		}

		this.slideTicks++;
		if (this.slideTicks > CombatRules.SLIDE_MAX_TICKS || !player.onGround() || player.isInWater() || player.isInLava() || player.getAbilities().flying) {
			this.stopSlide();
			return;
		}

		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(this.slideX * CombatRules.SLIDE_SPEED, velocity.y, this.slideZ * CombatRules.SLIDE_SPEED);
	}

	public boolean dashReady() {
		return this.dashCooldown <= 0;
	}

	public int dashCooldown() {
		return this.dashCooldown;
	}

	public int dashCooldownMax() {
		return this.dashCooldownMax;
	}

	public void startDashCooldown() {
		this.startDashCooldown(CombatRules.DASH_COOLDOWN);
	}

	public void startDashCooldown(int ticks) {
		this.dashCooldown = ticks;
		this.dashCooldownMax = Math.max(1, ticks);
	}

	public boolean doubleJumpReady() {
		return this.doubleJumpReady;
	}

	public void useDoubleJump() {
		this.doubleJumpReady = false;
		this.groundTicks = 0;
	}

	public boolean slideReady() {
		return this.slideCooldown <= 0 && !this.sliding;
	}

	public boolean sliding() {
		return this.sliding;
	}

	public int slideCooldown() {
		return this.slideCooldown;
	}

	public void startSlide(Vec3 direction) {
		this.sliding = true;
		this.slideTicks = 0;
		this.slideX = direction.x;
		this.slideZ = direction.z;
		this.slideHits.clear();
	}

	public void stopSlide() {
		if (!this.sliding) {
			return;
		}
		this.sliding = false;
		this.slideTicks = 0;
		this.slideCooldown = CombatRules.SLIDE_COOLDOWN;
		this.slideHits.clear();
	}

	public double slideX() {
		return this.slideX;
	}

	public double slideZ() {
		return this.slideZ;
	}

	public boolean markSlideHit(UUID id) {
		return this.slideHits.add(id);
	}

	public boolean thrustReady() {
		return this.thrustCooldown <= 0;
	}

	public void startThrustCooldown() {
		this.thrustCooldown = CombatRules.THRUST_COOLDOWN;
	}

	public boolean heavyReady() {
		return this.heavyCooldown <= 0 && !this.heavyCharging && this.heavyRecovery <= 0;
	}

	public boolean heavyLocked() {
		return this.heavyCharging || this.heavyRecovery > 0;
	}

	public boolean heavyCharging() {
		return this.heavyCharging;
	}

	public long heavyStartedAt() {
		return this.heavyStartedAt;
	}

	public void startHeavy(long gameTime) {
		this.heavyCharging = true;
		this.heavyStartedAt = gameTime;
	}

	public int consumeHeavy(long gameTime) {
		this.heavyCharging = false;
		return (int) Math.max(0L, gameTime - this.heavyStartedAt);
	}

	public void startHeavyCooldown() {
		this.heavyCooldown = CombatRules.HEAVY_COOLDOWN;
		this.heavyRecovery = CombatRules.HEAVY_RECOVERY_TICKS;
	}

	public void cancelHeavy() {
		this.heavyCharging = false;
	}

	public boolean parryReady() {
		return this.parryCooldown <= 0 && this.parryWindow <= 0;
	}

	public boolean parrying() {
		return this.parryWindow > 0;
	}

	public int parryWindow() {
		return this.parryWindow;
	}

	public boolean parryPerfect() {
		return this.parryWindow > CombatRules.PARRY_WINDOW - CombatRules.PARRY_PERFECT_TICKS;
	}

	public int parryCooldown() {
		return this.parryCooldown;
	}

	public int parryCooldownMax() {
		return this.parryCooldownMax;
	}

	public void startParry() {
		this.parryWindow = CombatRules.PARRY_WINDOW;
		this.parrySucceeded = false;
		this.parryResolved = false;
	}

	public void markParrySuccess() {
		this.parrySucceeded = true;
	}

	public boolean consumeParryResolution() {
		boolean resolved = this.parryResolved;
		this.parryResolved = false;
		return resolved;
	}

	public long shieldRaisedAt() {
		return this.shieldRaisedAt;
	}

	public boolean shieldRaised() {
		return this.shieldRaised;
	}

	public void setParryCooldown(int ticks) {
		this.parryWindow = 0;
		this.parryCooldown = ticks;
		this.parryCooldownMax = Math.max(1, ticks);
	}
}
