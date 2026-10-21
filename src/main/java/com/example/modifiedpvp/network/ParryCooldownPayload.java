package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ParryCooldownPayload(int ticks) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ParryCooldownPayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("parry_cooldown"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ParryCooldownPayload> CODEC = StreamCodec.of(
			(buf, payload) -> buf.writeVarInt(payload.ticks()),
			buf -> new ParryCooldownPayload(buf.readVarInt())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
