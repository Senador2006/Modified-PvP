package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.adaptive.AdaptiveStyle;
import com.example.modifiedpvp.combat.adaptive.FamilyPhase;
import com.example.modifiedpvp.combat.adaptive.WeaponFamily;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AdaptiveStatePayload(
		int familyId,
		int phaseId,
		int styleId,
		int progress,
		int readyTicks,
		int cooldownTicks,
		int cooldownMax
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdaptiveStatePayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("adaptive_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdaptiveStatePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeByte(payload.familyId());
				buf.writeByte(payload.phaseId());
				buf.writeByte(payload.styleId());
				buf.writeByte(payload.progress());
				buf.writeVarInt(payload.readyTicks());
				buf.writeVarInt(payload.cooldownTicks());
				buf.writeVarInt(payload.cooldownMax());
			},
			buf -> new AdaptiveStatePayload(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
	);

	public static AdaptiveStatePayload of(WeaponFamily family, FamilyPhase phase, AdaptiveStyle style, int progress, int readyTicks, int cooldownTicks, int cooldownMax) {
		return new AdaptiveStatePayload(
				family == null ? 255 : family.ordinal(),
				phase.ordinal(),
				style == null ? 255 : style.ordinal(),
				Math.max(0, Math.min(100, progress)),
				readyTicks,
				cooldownTicks,
				Math.max(1, cooldownMax));
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
