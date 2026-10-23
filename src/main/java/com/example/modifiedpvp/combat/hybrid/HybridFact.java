package com.example.modifiedpvp.combat.hybrid;

import java.util.UUID;

public record HybridFact(
		UUID actionId,
		long tick,
		HybridAction action,
		UUID targetId,
		UUID projectileId,
		int aerialId,
		boolean hit,
		boolean blocked,
		boolean attempt,
		boolean airborne,
		boolean validFall,
		boolean distanceIncreasing,
		boolean approaching,
		boolean loyalty,
		boolean hasPull,
		boolean tridentAway,
		boolean timed,
		boolean fullCharge,
		boolean hostile,
		boolean advancing,
		boolean lateSwap,
		boolean sequenceWind,
		boolean sequenceLunge,
		boolean sequenceSpear,
		boolean sequenceAerial,
		int affected
) {
	public static Builder builder(HybridAction action, long tick) {
		return new Builder(action, tick);
	}

	public boolean sameTarget(UUID target) {
		return target != null && target.equals(this.targetId);
	}

	public static final class Builder {
		private UUID actionId = UUID.randomUUID();
		private final long tick;
		private final HybridAction action;
		private UUID targetId;
		private UUID projectileId;
		private int aerialId = -1;
		private boolean hit = true;
		private boolean blocked;
		private boolean attempt = true;
		private boolean airborne;
		private boolean validFall;
		private boolean distanceIncreasing;
		private boolean approaching;
		private boolean loyalty;
		private boolean hasPull;
		private boolean tridentAway;
		private boolean timed;
		private boolean fullCharge;
		private boolean hostile;
		private boolean advancing;
		private boolean lateSwap;
		private boolean sequenceWind;
		private boolean sequenceLunge;
		private boolean sequenceSpear;
		private boolean sequenceAerial;
		private int affected = 1;

		private Builder(HybridAction action, long tick) {
			this.action = action;
			this.tick = tick;
		}

		public Builder actionId(UUID actionId) {
			this.actionId = actionId;
			return this;
		}

		public Builder target(UUID targetId) {
			this.targetId = targetId;
			return this;
		}

		public Builder projectile(UUID projectileId) {
			this.projectileId = projectileId;
			return this;
		}

		public Builder aerial(int aerialId) {
			this.aerialId = aerialId;
			return this;
		}

		public Builder hit(boolean hit) {
			this.hit = hit;
			return this;
		}

		public Builder blocked(boolean blocked) {
			this.blocked = blocked;
			return this;
		}

		public Builder attempt(boolean attempt) {
			this.attempt = attempt;
			return this;
		}

		public Builder airborne(boolean airborne) {
			this.airborne = airborne;
			return this;
		}

		public Builder validFall(boolean validFall) {
			this.validFall = validFall;
			return this;
		}

		public Builder distanceIncreasing(boolean distanceIncreasing) {
			this.distanceIncreasing = distanceIncreasing;
			return this;
		}

		public Builder approaching(boolean approaching) {
			this.approaching = approaching;
			return this;
		}

		public Builder loyalty(boolean loyalty) {
			this.loyalty = loyalty;
			return this;
		}

		public Builder pull(boolean hasPull) {
			this.hasPull = hasPull;
			return this;
		}

		public Builder tridentAway(boolean tridentAway) {
			this.tridentAway = tridentAway;
			return this;
		}

		public Builder timed(boolean timed) {
			this.timed = timed;
			return this;
		}

		public Builder fullCharge(boolean fullCharge) {
			this.fullCharge = fullCharge;
			return this;
		}

		public Builder hostile(boolean hostile) {
			this.hostile = hostile;
			return this;
		}

		public Builder advancing(boolean advancing) {
			this.advancing = advancing;
			return this;
		}

		public Builder lateSwap(boolean lateSwap) {
			this.lateSwap = lateSwap;
			return this;
		}

		public Builder sequenceWind(boolean sequenceWind) {
			this.sequenceWind = sequenceWind;
			return this;
		}

		public Builder sequenceLunge(boolean sequenceLunge) {
			this.sequenceLunge = sequenceLunge;
			return this;
		}

		public Builder sequenceSpear(boolean sequenceSpear) {
			this.sequenceSpear = sequenceSpear;
			return this;
		}

		public Builder sequenceAerial(boolean sequenceAerial) {
			this.sequenceAerial = sequenceAerial;
			return this;
		}

		public Builder affected(int affected) {
			this.affected = affected;
			return this;
		}

		public HybridFact build() {
			return new HybridFact(
					this.actionId,
					this.tick,
					this.action,
					this.targetId,
					this.projectileId,
					this.aerialId,
					this.hit,
					this.blocked,
					this.attempt,
					this.airborne,
					this.validFall,
					this.distanceIncreasing,
					this.approaching,
					this.loyalty,
					this.hasPull,
					this.tridentAway,
					this.timed,
					this.fullCharge,
					this.hostile,
					this.advancing,
					this.lateSwap,
					this.sequenceWind,
					this.sequenceLunge,
					this.sequenceSpear,
					this.sequenceAerial,
					Math.max(0, this.affected));
		}
	}
}
