package com.example.modifiedpvp.combat;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent PARRY = register("parry");

	private ModSounds() {
	}

	public static void register() {
	}

	private static SoundEvent register(String path) {
		Identifier id = ModifiedPvP.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
