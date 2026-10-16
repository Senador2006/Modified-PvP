package com.example.modifiedpvp.mixin;

import com.mojang.math.Transformation;

import net.minecraft.world.entity.Display;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayInvoker {
	@Invoker("setPosRotInterpolationDuration")
	void modifiedpvp$setPosRotInterpolationDuration(int ticks);

	@Invoker("setBillboardConstraints")
	void modifiedpvp$setBillboardConstraints(Display.BillboardConstraints constraints);

	@Invoker("setViewRange")
	void modifiedpvp$setViewRange(float range);

	@Invoker("setTransformation")
	void modifiedpvp$setTransformation(Transformation transformation);
}
