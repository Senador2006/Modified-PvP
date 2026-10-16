package com.example.modifiedpvp.network;

import com.example.modifiedpvp.ModifiedPvP;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CombatActionPayload(CombatAction action) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CombatActionPayload> TYPE = new CustomPacketPayload.Type<>(ModifiedPvP.id("combat_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CombatActionPayload> CODEC = StreamCodec.of(
			(buf, payload) -> buf.writeByte(payload.action().id()),
			buf -> new CombatActionPayload(CombatAction.byId(buf.readUnsignedByte()))
	);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
