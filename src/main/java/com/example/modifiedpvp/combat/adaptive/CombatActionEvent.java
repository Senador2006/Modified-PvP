package com.example.modifiedpvp.combat.adaptive;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record CombatActionEvent(
		UUID actionId,
		UUID playerId,
		WeaponFamily family,
		CombatActionType type,
		long serverTick,
		UUID targetId,
		Set<ContextTag> context,
		int affectedTargets,
		boolean hitValid,
		boolean blocked,
		boolean perfectTiming
) {
	public CombatActionEvent {
		context = context == null || context.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(context));
		affectedTargets = Math.max(0, affectedTargets);
	}

	public boolean has(ContextTag tag) {
		return this.context.contains(tag);
	}

	public boolean sameTarget(UUID other) {
		return this.targetId != null && this.targetId.equals(other);
	}

	public static Builder builder(UUID playerId, WeaponFamily family, CombatActionType type, long tick) {
		return new Builder(playerId, family, type, tick);
	}

	public static final class Builder {
		private UUID actionId = UUID.randomUUID();
		private final UUID playerId;
		private final WeaponFamily family;
		private final CombatActionType type;
		private final long serverTick;
		private UUID targetId;
		private final EnumSet<ContextTag> context = EnumSet.noneOf(ContextTag.class);
		private int affectedTargets = 1;
		private boolean hitValid = true;
		private boolean blocked;
		private boolean perfectTiming;

		private Builder(UUID playerId, WeaponFamily family, CombatActionType type, long tick) {
			this.playerId = playerId;
			this.family = family;
			this.type = type;
			this.serverTick = tick;
		}

		public Builder actionId(UUID actionId) {
			this.actionId = actionId;
			return this;
		}

		public Builder target(UUID targetId) {
			this.targetId = targetId;
			return this;
		}

		public Builder tag(ContextTag tag) {
			this.context.add(tag);
			return this;
		}

		public Builder tags(Set<ContextTag> tags) {
			this.context.addAll(tags);
			return this;
		}

		public Builder affected(int count) {
			this.affectedTargets = count;
			return this;
		}

		public Builder hit(boolean valid) {
			this.hitValid = valid;
			return this;
		}

		public Builder blocked(boolean blocked) {
			this.blocked = blocked;
			return this;
		}

		public Builder perfect(boolean perfect) {
			this.perfectTiming = perfect;
			if (perfect) {
				this.context.add(ContextTag.PERFECT_TIMING);
			}
			return this;
		}

		public CombatActionEvent build() {
			return new CombatActionEvent(
					this.actionId,
					this.playerId,
					this.family,
					this.type,
					this.serverTick,
					this.targetId,
					this.context,
					this.affectedTargets,
					this.hitValid,
					this.blocked,
					this.perfectTiming);
		}
	}
}
