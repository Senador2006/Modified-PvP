package com.example.modifiedpvp.client;

import com.example.modifiedpvp.ModifiedPvP;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

public final class ModKeybinds {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ModifiedPvP.id("combat"));
	public static final KeyMapping DASH = register("dash", GLFW.GLFW_KEY_R);
	public static final KeyMapping SLIDE = register("slide", GLFW.GLFW_KEY_C);
	public static final KeyMapping THRUST = register("thrust", GLFW.GLFW_KEY_V);
	public static final KeyMapping CYCLE_ARROW = register("cycle_arrow", GLFW.GLFW_KEY_G);
	public static final KeyMapping PARRY = register("parry", GLFW.GLFW_KEY_X);

	private ModKeybinds() {
	}

	public static void register() {
		KeyBindingHelper.registerKeyBinding(DASH);
		KeyBindingHelper.registerKeyBinding(SLIDE);
		KeyBindingHelper.registerKeyBinding(THRUST);
		KeyBindingHelper.registerKeyBinding(CYCLE_ARROW);
		KeyBindingHelper.registerKeyBinding(PARRY);
	}

	private static KeyMapping register(String path, int key) {
		return new KeyMapping("key.modifiedpvp." + path, key, CATEGORY);
	}
}
