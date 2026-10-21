package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SpearStatePayload(int guardTicks, int perfectTicks, int cooldownTicks, int cooldownMax, int riposteTicks, int followUpTicks, int finisherCooldownTicks, int selfBrokenTicks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpearStatePayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("spear_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SpearStatePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.guardTicks());
				buf.writeVarInt(payload.perfectTicks());
				buf.writeVarInt(payload.cooldownTicks());
				buf.writeVarInt(payload.cooldownMax());
				buf.writeVarInt(payload.riposteTicks());
				buf.writeVarInt(payload.followUpTicks());
				buf.writeVarInt(payload.finisherCooldownTicks());
				buf.writeVarInt(payload.selfBrokenTicks());
			},
			buf -> new SpearStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
