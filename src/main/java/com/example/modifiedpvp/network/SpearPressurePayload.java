package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SpearPressurePayload(int entityId, int pressure, int brokenTicks, boolean enhanced, boolean immune) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpearPressurePayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("spear_pressure"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SpearPressurePayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.entityId());
				buf.writeVarInt(payload.pressure());
				buf.writeVarInt(payload.brokenTicks());
				buf.writeBoolean(payload.enhanced());
				buf.writeBoolean(payload.immune());
			},
			buf -> new SpearPressurePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean())
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
