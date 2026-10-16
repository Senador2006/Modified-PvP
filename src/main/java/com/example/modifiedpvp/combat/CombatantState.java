package com.example.modifiedpvp.combat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class CombatantState {
	private int dashCooldown;
	private int thrustCooldown;
	private int heavyCooldown;
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

	public void tick(Player player) {
		if (this.dashCooldown > 0) {
			this.dashCooldown--;
		}
		if (this.thrustCooldown > 0) {
			this.thrustCooldown--;
		}
		if (this.heavyCooldown > 0) {
			this.heavyCooldown--;
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

	public void startDashCooldown() {
		this.dashCooldown = CombatRules.DASH_COOLDOWN;
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
		return this.heavyCooldown <= 0 && !this.heavyCharging;
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
	}

	public void cancelHeavy() {
		this.heavyCharging = false;
	}
}
