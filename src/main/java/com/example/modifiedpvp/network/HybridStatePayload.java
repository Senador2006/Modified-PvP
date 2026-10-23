package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record HybridStatePayload(
		int pairId,
		int styleId,
		int phaseId,
		int cooldown,
		int cooldownMax,
		boolean crown
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HybridStatePayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("hybrid_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, HybridStatePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeByte(payload.pairId());
				buf.writeByte(payload.styleId());
				buf.writeByte(payload.phaseId());
				buf.writeVarInt(payload.cooldown());
				buf.writeVarInt(payload.cooldownMax());
				buf.writeBoolean(payload.crown());
			},
			buf -> new HybridStatePayload(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
