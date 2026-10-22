package com.example.modifiedpvp.combat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

final class ComboData {
	static final Codec<ComboData> CODEC = CompoundTag.CODEC.xmap(ComboData::fromTag, ComboData::toTag);
	static final StreamCodec<RegistryFriendlyByteBuf, ComboData> STREAM_CODEC = StreamCodec.of(ComboData::encode, ComboData::decode);

	int swordKills;
	int dualKills;
	int heavyHits;
	final Set<UUID> bowHits = new LinkedHashSet<>();

	int swordCombo;
	long swordWindowUntil;
	long swordCooldownUntil;
	long swordBuffUntil;
	int swordVariety;
	int swordLastAction;

	int dualCombo;
	long dualWindowUntil;
	long dualCooldownUntil;
	long dualBuffUntil;
	int dualLastHand;

	int axeCombo;
	long axeWindowUntil;
	long axeCooldownUntil;
	boolean axeEmpowered;

	int bowCombo;
	long bowWindowUntil;
	long bowCooldownUntil;
	long bowBuffUntil;
	int bowShotsLeft;

	int tridentCombo;
	long tridentWindowUntil;
	long tridentCooldownUntil;

	int shieldBlocks;
	int shieldCombo;
	long shieldWindowUntil;
	long shieldCooldownUntil;
	boolean shieldEmpowered;

	int chainHits;
	int maceCombo;
	long maceWindowUntil;
	long maceCooldownUntil;
	boolean maceEmpowered;

	ComboData copy() {
		ComboData copy = new ComboData();
		copy.swordKills = this.swordKills;
		copy.dualKills = this.dualKills;
		copy.heavyHits = this.heavyHits;
		copy.bowHits.addAll(this.bowHits);
		copy.swordCombo = this.swordCombo;
		copy.swordWindowUntil = this.swordWindowUntil;
		copy.swordCooldownUntil = this.swordCooldownUntil;
		copy.swordBuffUntil = this.swordBuffUntil;
		copy.swordVariety = this.swordVariety;
		copy.swordLastAction = this.swordLastAction;
		copy.dualCombo = this.dualCombo;
		copy.dualWindowUntil = this.dualWindowUntil;
		copy.dualCooldownUntil = this.dualCooldownUntil;
		copy.dualBuffUntil = this.dualBuffUntil;
		copy.dualLastHand = this.dualLastHand;
		copy.axeCombo = this.axeCombo;
		copy.axeWindowUntil = this.axeWindowUntil;
		copy.axeCooldownUntil = this.axeCooldownUntil;
		copy.axeEmpowered = this.axeEmpowered;
		copy.bowCombo = this.bowCombo;
		copy.bowWindowUntil = this.bowWindowUntil;
		copy.bowCooldownUntil = this.bowCooldownUntil;
		copy.bowBuffUntil = this.bowBuffUntil;
		copy.bowShotsLeft = this.bowShotsLeft;
		copy.tridentCombo = this.tridentCombo;
		copy.tridentWindowUntil = this.tridentWindowUntil;
		copy.tridentCooldownUntil = this.tridentCooldownUntil;
		copy.shieldBlocks = this.shieldBlocks;
		copy.shieldCombo = this.shieldCombo;
		copy.shieldWindowUntil = this.shieldWindowUntil;
		copy.shieldCooldownUntil = this.shieldCooldownUntil;
		copy.shieldEmpowered = this.shieldEmpowered;
		copy.chainHits = this.chainHits;
		copy.maceCombo = this.maceCombo;
		copy.maceWindowUntil = this.maceWindowUntil;
		copy.maceCooldownUntil = this.maceCooldownUntil;
		copy.maceEmpowered = this.maceEmpowered;
		return copy;
	}

	void clearCombat() {
		this.swordCombo = 0;
		this.swordWindowUntil = 0L;
		this.swordBuffUntil = 0L;
		this.swordVariety = 0;
		this.swordLastAction = 0;
		this.dualCombo = 0;
		this.dualWindowUntil = 0L;
		this.dualBuffUntil = 0L;
		this.dualLastHand = 0;
		this.axeCombo = 0;
		this.axeWindowUntil = 0L;
		this.axeEmpowered = false;
		this.bowCombo = 0;
		this.bowWindowUntil = 0L;
		this.bowBuffUntil = 0L;
		this.bowShotsLeft = 0;
		this.tridentCombo = 0;
		this.tridentWindowUntil = 0L;
		this.shieldCombo = 0;
		this.shieldWindowUntil = 0L;
		this.shieldEmpowered = false;
		this.maceCombo = 0;
		this.maceWindowUntil = 0L;
		this.maceEmpowered = false;
	}

	private static ComboData fromTag(CompoundTag tag) {
		ComboData data = new ComboData();
		data.swordKills = tag.getIntOr("sword_kills", 0);
		data.dualKills = tag.getIntOr("dual_kills", 0);
		data.heavyHits = tag.getIntOr("heavy_hits", 0);
		tag.read("bow_hits", UUIDUtil.CODEC.listOf()).ifPresent(data.bowHits::addAll);
		data.swordCombo = tag.getIntOr("sword_combo", 0);
		data.swordWindowUntil = tag.getLongOr("sword_window", 0L);
		data.swordCooldownUntil = tag.getLongOr("sword_cooldown", 0L);
		data.swordBuffUntil = tag.getLongOr("sword_buff", 0L);
		data.swordVariety = tag.getIntOr("sword_variety", 0);
		data.swordLastAction = tag.getIntOr("sword_last", 0);
		data.dualCombo = tag.getIntOr("dual_combo", 0);
		data.dualWindowUntil = tag.getLongOr("dual_window", 0L);
		data.dualCooldownUntil = tag.getLongOr("dual_cooldown", 0L);
		data.dualBuffUntil = tag.getLongOr("dual_buff", 0L);
		data.dualLastHand = tag.getIntOr("dual_last", 0);
		data.axeCombo = tag.getIntOr("axe_combo", 0);
		data.axeWindowUntil = tag.getLongOr("axe_window", 0L);
		data.axeCooldownUntil = tag.getLongOr("axe_cooldown", 0L);
		data.axeEmpowered = tag.getBooleanOr("axe_empowered", false);
		data.bowCombo = tag.getIntOr("bow_combo", 0);
		data.bowWindowUntil = tag.getLongOr("bow_window", 0L);
		data.bowCooldownUntil = tag.getLongOr("bow_cooldown", 0L);
		data.bowBuffUntil = tag.getLongOr("bow_buff", 0L);
		data.bowShotsLeft = tag.getIntOr("bow_shots", 0);
		data.tridentCombo = tag.getIntOr("trident_combo", 0);
		data.tridentWindowUntil = tag.getLongOr("trident_window", 0L);
		data.tridentCooldownUntil = tag.getLongOr("trident_cooldown", 0L);
		data.shieldBlocks = tag.getIntOr("shield_blocks", 0);
		data.shieldCombo = tag.getIntOr("shield_combo", 0);
		data.shieldWindowUntil = tag.getLongOr("shield_window", 0L);
		data.shieldCooldownUntil = tag.getLongOr("shield_cooldown", 0L);
		data.shieldEmpowered = tag.getBooleanOr("shield_empowered", false);
		data.chainHits = tag.getIntOr("chain_hits", 0);
		data.maceCombo = tag.getIntOr("mace_combo", 0);
		data.maceWindowUntil = tag.getLongOr("mace_window", 0L);
		data.maceCooldownUntil = tag.getLongOr("mace_cooldown", 0L);
		data.maceEmpowered = tag.getBooleanOr("mace_empowered", false);
		return data;
	}

	private static CompoundTag toTag(ComboData data) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("sword_kills", data.swordKills);
		tag.putInt("dual_kills", data.dualKills);
		tag.putInt("heavy_hits", data.heavyHits);
		tag.store("bow_hits", UUIDUtil.CODEC.listOf(), List.copyOf(data.bowHits));
		tag.putInt("sword_combo", data.swordCombo);
		tag.putLong("sword_window", data.swordWindowUntil);
		tag.putLong("sword_cooldown", data.swordCooldownUntil);
		tag.putLong("sword_buff", data.swordBuffUntil);
		tag.putInt("sword_variety", data.swordVariety);
		tag.putInt("sword_last", data.swordLastAction);
		tag.putInt("dual_combo", data.dualCombo);
		tag.putLong("dual_window", data.dualWindowUntil);
		tag.putLong("dual_cooldown", data.dualCooldownUntil);
		tag.putLong("dual_buff", data.dualBuffUntil);
		tag.putInt("dual_last", data.dualLastHand);
		tag.putInt("axe_combo", data.axeCombo);
		tag.putLong("axe_window", data.axeWindowUntil);
		tag.putLong("axe_cooldown", data.axeCooldownUntil);
		tag.putBoolean("axe_empowered", data.axeEmpowered);
		tag.putInt("bow_combo", data.bowCombo);
		tag.putLong("bow_window", data.bowWindowUntil);
		tag.putLong("bow_cooldown", data.bowCooldownUntil);
		tag.putLong("bow_buff", data.bowBuffUntil);
		tag.putInt("bow_shots", data.bowShotsLeft);
		tag.putInt("trident_combo", data.tridentCombo);
		tag.putLong("trident_window", data.tridentWindowUntil);
		tag.putLong("trident_cooldown", data.tridentCooldownUntil);
		tag.putInt("shield_blocks", data.shieldBlocks);
		tag.putInt("shield_combo", data.shieldCombo);
		tag.putLong("shield_window", data.shieldWindowUntil);
		tag.putLong("shield_cooldown", data.shieldCooldownUntil);
		tag.putBoolean("shield_empowered", data.shieldEmpowered);
		tag.putInt("chain_hits", data.chainHits);
		tag.putInt("mace_combo", data.maceCombo);
		tag.putLong("mace_window", data.maceWindowUntil);
		tag.putLong("mace_cooldown", data.maceCooldownUntil);
		tag.putBoolean("mace_empowered", data.maceEmpowered);
		return tag;
	}

	private static void encode(RegistryFriendlyByteBuf buf, ComboData data) {
		buf.writeVarInt(data.swordKills);
		buf.writeVarInt(data.dualKills);
		buf.writeVarInt(data.heavyHits);
		buf.writeVarInt(data.bowHits.size());
		for (UUID id : data.bowHits) {
			buf.writeUUID(id);
		}
		buf.writeVarInt(data.swordCombo);
		buf.writeLong(data.swordWindowUntil);
		buf.writeLong(data.swordCooldownUntil);
		buf.writeLong(data.swordBuffUntil);
		buf.writeVarInt(data.swordVariety);
		buf.writeVarInt(data.swordLastAction);
		buf.writeVarInt(data.dualCombo);
		buf.writeLong(data.dualWindowUntil);
		buf.writeLong(data.dualCooldownUntil);
		buf.writeLong(data.dualBuffUntil);
		buf.writeVarInt(data.dualLastHand);
		buf.writeVarInt(data.axeCombo);
		buf.writeLong(data.axeWindowUntil);
		buf.writeLong(data.axeCooldownUntil);
		buf.writeBoolean(data.axeEmpowered);
		buf.writeVarInt(data.bowCombo);
		buf.writeLong(data.bowWindowUntil);
		buf.writeLong(data.bowCooldownUntil);
		buf.writeLong(data.bowBuffUntil);
		buf.writeVarInt(data.bowShotsLeft);
		buf.writeVarInt(data.tridentCombo);
		buf.writeLong(data.tridentWindowUntil);
		buf.writeLong(data.tridentCooldownUntil);
		buf.writeVarInt(data.shieldBlocks);
		buf.writeVarInt(data.shieldCombo);
		buf.writeLong(data.shieldWindowUntil);
		buf.writeLong(data.shieldCooldownUntil);
		buf.writeBoolean(data.shieldEmpowered);
		buf.writeVarInt(data.chainHits);
		buf.writeVarInt(data.maceCombo);
		buf.writeLong(data.maceWindowUntil);
		buf.writeLong(data.maceCooldownUntil);
		buf.writeBoolean(data.maceEmpowered);
	}

	private static ComboData decode(RegistryFriendlyByteBuf buf) {
		ComboData data = new ComboData();
		data.swordKills = buf.readVarInt();
		data.dualKills = buf.readVarInt();
		data.heavyHits = buf.readVarInt();
		int hits = buf.readVarInt();
		for (int i = 0; i < hits; i++) {
			data.bowHits.add(buf.readUUID());
		}
		data.swordCombo = buf.readVarInt();
		data.swordWindowUntil = buf.readLong();
		data.swordCooldownUntil = buf.readLong();
		data.swordBuffUntil = buf.readLong();
		data.swordVariety = buf.readVarInt();
		data.swordLastAction = buf.readVarInt();
		data.dualCombo = buf.readVarInt();
		data.dualWindowUntil = buf.readLong();
		data.dualCooldownUntil = buf.readLong();
		data.dualBuffUntil = buf.readLong();
		data.dualLastHand = buf.readVarInt();
		data.axeCombo = buf.readVarInt();
		data.axeWindowUntil = buf.readLong();
		data.axeCooldownUntil = buf.readLong();
		data.axeEmpowered = buf.readBoolean();
		data.bowCombo = buf.readVarInt();
		data.bowWindowUntil = buf.readLong();
		data.bowCooldownUntil = buf.readLong();
		data.bowBuffUntil = buf.readLong();
		data.bowShotsLeft = buf.readVarInt();
		data.tridentCombo = buf.readVarInt();
		data.tridentWindowUntil = buf.readLong();
		data.tridentCooldownUntil = buf.readLong();
		data.shieldBlocks = buf.readVarInt();
		data.shieldCombo = buf.readVarInt();
		data.shieldWindowUntil = buf.readLong();
		data.shieldCooldownUntil = buf.readLong();
		data.shieldEmpowered = buf.readBoolean();
		data.chainHits = buf.readVarInt();
		data.maceCombo = buf.readVarInt();
		data.maceWindowUntil = buf.readLong();
		data.maceCooldownUntil = buf.readLong();
		data.maceEmpowered = buf.readBoolean();
		return data;
	}
}
