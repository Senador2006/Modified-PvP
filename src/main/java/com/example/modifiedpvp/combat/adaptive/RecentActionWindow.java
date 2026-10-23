package com.example.modifiedpvp.combat.adaptive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RecentActionWindow {
	private final List<CombatActionEvent> events = new ArrayList<>();
	private final Set<UUID> seen = new HashSet<>();

	public boolean remember(CombatActionEvent event, int maxSize, long minTick) {
		if (!this.seen.add(event.actionId())) {
			return false;
		}
		this.events.add(event);
		this.trim(maxSize, minTick);
		return true;
	}

	public void trim(int maxSize, long minTick) {
		Iterator<CombatActionEvent> iterator = this.events.iterator();
		while (iterator.hasNext()) {
			CombatActionEvent event = iterator.next();
			if (event.serverTick() < minTick) {
				iterator.remove();
				this.seen.remove(event.actionId());
			}
		}
		while (this.events.size() > maxSize) {
			CombatActionEvent oldest = this.events.removeFirst();
			this.seen.remove(oldest.actionId());
		}
	}

	public boolean seen(UUID actionId) {
		return this.seen.contains(actionId);
	}

	public List<CombatActionEvent> events() {
		return List.copyOf(this.events);
	}

	public CombatActionEvent last() {
		return this.events.isEmpty() ? null : this.events.getLast();
	}

	public void clear() {
		this.events.clear();
		this.seen.clear();
	}

	public int size() {
		return this.events.size();
	}
}
