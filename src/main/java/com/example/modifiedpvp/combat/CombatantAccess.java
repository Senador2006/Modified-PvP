package com.example.modifiedpvp.combat;

import net.minecraft.world.entity.player.Player;

public interface CombatantAccess {
	CombatantState modifiedpvp$state();

	static CombatantState of(Player player) {
		return ((CombatantAccess) player).modifiedpvp$state();
	}
}
