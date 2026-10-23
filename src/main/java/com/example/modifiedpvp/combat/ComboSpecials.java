package com.example.modifiedpvp.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.modifiedpvp.ModifiedPvP;
import com.example.modifiedpvp.combat.adaptive.AdaptiveCombatManager;
import com.example.modifiedpvp.combat.adaptive.AdaptiveCombatState;
import com.example.modifiedpvp.combat.adaptive.AdaptiveStyle;
import com.example.modifiedpvp.combat.adaptive.CombatActionType;
import com.example.modifiedpvp.combat.adaptive.ContextTag;
import com.example.modifiedpvp.combat.adaptive.FamilyCombatState;
import com.example.modifiedpvp.combat.adaptive.FamilyPhase;
import com.example.modifiedpvp.combat.adaptive.FinisherEffects;
import com.example.modifiedpvp.combat.adaptive.WeaponFamily;
import com.example.modifiedpvp.combat.hybrid.HybridCombatManager;
import com.example.modifiedpvp.combat.hybrid.HybridResult;
import com.example.modifiedpvp.combat.hybrid.HybridStyleId;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ComboSpecials {
	private static final AttachmentType<ComboData> ATTACHMENT = AttachmentRegistry.create(ModifiedPvP.id("combo_data"), builder -> builder
			.persistent(ComboData.CODEC)
			.copyOnDeath()
			.syncWith(ComboData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	private static final Map<UUID, Long> CART_COMBO_GRACE = new HashMap<>();
	private static final int CART_COMBO_GRACE_TICKS = 60;
	private static final List<PendingSlow> PENDING_SLOWS = new ArrayList<>();
	private static final List<Stun> STUNS = new ArrayList<>();
	private static final ArrayDeque<HurtFrame> HURTS = new ArrayDeque<>();
	private static boolean reflecting;

	private ComboSpecials() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DEATH.register(ComboSpecials::onDeath);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player != null) {
				clearCombat(handler.player);
				AdaptiveCombatManager.clear(handler.player);
				AdaptiveCombatManager.forget(handler.player.getUUID());
				HybridCombatManager.clear(handler.player);
				ControlGuard.forget(handler.player.getUUID());
			}
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			clearCombat(player);
			AdaptiveCombatManager.clear(player);
			HybridCombatManager.clear(player);
		});
	}

	public static void tick() {
		tickSlows();
		tickStuns();
	}

	public static void tickPlayer(ServerPlayer player) {
		if (player.tickCount % 8 != 0 || !isGuarded(player)) {
			return;
		}
		player.level().sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(0.6), player.getZ(), 2, 0.25, 0.3, 0.25, 0.0);
	}

	public static void onMeleeHit(ServerPlayer player) {
		onMeleeHit(player, null, false);
	}

	public static void onMeleeHit(ServerPlayer player, LivingEntity target, boolean blocked) {
		if (WeaponStats.isDualWielding(player)) {
			AdaptiveCombatManager.record(player, WeaponFamily.DUAL_SWORD, CombatActionType.MELEE_HIT, target, true, blocked, false, 1, EnumSet.noneOf(ContextTag.class));
			return;
		}
		if (WeaponStats.isSword(player.getMainHandItem())) {
			HybridResult hybrid = HybridCombatManager.onSwordHit(player, target, true, blocked);
			if (hybrid.exclusive()) {
				return;
			}
			EnumSet<ContextTag> tags = EnumSet.noneOf(ContextTag.class);
			if (AdaptiveCombatManager.isReady(player, WeaponFamily.SWORD, AdaptiveStyle.RESPOSTA_LAMINA)) {
				tags.add(ContextTag.COUNTER);
			}
			AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.SWORD, CombatActionType.MELEE_HIT, target, true, blocked, false, 1, tags);
			if (consumed == AdaptiveStyle.RESPOSTA_LAMINA && target != null) {
				FinisherEffects.shove(player, target, 0.25, AdaptiveCombatManager.config());
				FinisherEffects.burst(target, 8);
			}
		} else if (player.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT)) {
			if (player.isAutoSpinAttack()) {
				onRiptideHit(player, target);
			} else {
				AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.TRIDENT, CombatActionType.TRIDENT_MELEE_HIT, target, true, blocked, false, 1, EnumSet.noneOf(ContextTag.class));
				if (consumed == AdaptiveStyle.DOMINIO_MARES && target != null) {
					FinisherEffects.shove(player, target, AdaptiveCombatManager.config().tridentShove(), AdaptiveCombatManager.config());
				}
			}
		}
	}

	public static void onThrustHit(ServerPlayer player) {
		onThrustHit(player, null, true);
	}

	public static void onThrustHit(ServerPlayer player, LivingEntity target, boolean hit) {
		HybridResult hybrid = HybridCombatManager.onThrust(player, target, hit);
		if (hybrid.exclusive()) {
			return;
		}
		WeaponFamily family = WeaponStats.isDualWielding(player) ? WeaponFamily.DUAL_SWORD : WeaponFamily.SWORD;
		AdaptiveStyle consumed = AdaptiveCombatManager.record(player, family, CombatActionType.THRUST_HIT, target, hit, false, false, 1, EnumSet.noneOf(ContextTag.class));
		if (consumed == AdaptiveStyle.PONTA_PRECISAO && target != null) {
			FinisherEffects.burst(target, 6);
		}
	}

	public static void onCombinedHit(ServerPlayer player, LivingEntity target, boolean hit, boolean blocked) {
		if (!WeaponStats.isDualWielding(player)) {
			return;
		}
		if (!hit) {
			ControlGuard.interrupt(player, player, CombatRules.DUAL_EXPOSE_TICKS);
			AdaptiveCombatManager.record(player, WeaponFamily.DUAL_SWORD, CombatActionType.OFFHAND_COMBINED_HIT, target, false, blocked, false, 1, EnumSet.noneOf(ContextTag.class));
			return;
		}
		AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.DUAL_SWORD, CombatActionType.OFFHAND_COMBINED_HIT, target, true, blocked, false, 1, EnumSet.noneOf(ContextTag.class));
		if (consumed != null && target != null) {
			FinisherEffects.burst(target, consumed == AdaptiveStyle.ALTERNANCIA_PERFEITA ? 10 : 6);
		}
	}

	public static void onOffhandHit(ServerPlayer player, boolean hit) {
		onCombinedHit(player, null, hit, false);
	}

	public static void onPerfectParry(ServerPlayer player, LivingEntity attacker) {
		if (WeaponStats.isSword(player.getMainHandItem())) {
			WeaponFamily family = WeaponStats.isDualWielding(player) ? WeaponFamily.DUAL_SWORD : WeaponFamily.SWORD;
			AdaptiveCombatManager.record(player, family, CombatActionType.PERFECT_COUNTER, attacker, true, false, true, 1, EnumSet.of(ContextTag.COUNTER));
		}
	}

	public static void onHeavySuccess(ServerPlayer player, boolean comboHit) {
		ComboData data = edit(player);
		boolean dirty = false;
		if (data.heavyHits < CombatRules.HEAVY_GOAL) {
			data.heavyHits++;
			dirty = true;
			if (data.heavyHits >= CombatRules.HEAVY_GOAL) {
				grant(player, Style.AXE);
			} else {
				goal(player, Style.AXE, data.heavyHits, CombatRules.HEAVY_GOAL);
			}
		}
		if (comboHit && unlocked(player, Style.AXE)) {
			AdaptiveCombatManager.record(player, WeaponFamily.AXE, CombatActionType.HEAVY_FULL_HIT, null, true, false, false, 1, EnumSet.noneOf(ContextTag.class));
		}
		if (dirty) {
			save(player, data);
		}
	}

	public static void onBowHit(ServerPlayer player, LivingEntity target) {
		if (!(target instanceof Mob) && !(target instanceof Player)) {
			return;
		}
		ComboData data = edit(player);
		boolean dirty = false;
		if (target instanceof Mob && data.bowHits.size() < CombatRules.BOW_MOB_GOAL && data.bowHits.add(target.getUUID())) {
			dirty = true;
			if (data.bowHits.size() >= CombatRules.BOW_MOB_GOAL) {
				grant(player, Style.BOW);
			} else {
				goal(player, Style.BOW, data.bowHits.size(), CombatRules.BOW_MOB_GOAL);
			}
		}
		if (unlocked(player, Style.BOW)) {
			ComboData bowData = player.getAttached(ATTACHMENT);
			if (bowData != null && bowData.bowShotsLeft > 0) {
				FinisherEffects.shove(player, target, AdaptiveCombatManager.config().bowKnockback(), AdaptiveCombatManager.config());
				ComboData clear = edit(player);
				clear.bowShotsLeft = 0;
				save(player, clear);
			} else {
				AdaptiveCombatManager.record(player, WeaponFamily.BOW, CombatActionType.BOW_ARROW_HIT, target, true, false, false, 1, EnumSet.noneOf(ContextTag.class));
			}
		}
		if (dirty) {
			save(player, data);
		}
	}

	public static void onTridentReturn(ServerPlayer player, UUID projectileId) {
		if (!(player.level() instanceof ServerLevel)) {
			return;
		}
		AdaptiveCombatManager.record(player, WeaponFamily.TRIDENT, CombatActionType.TRIDENT_LOYALTY_RETURN, null, true, false, false, 1, EnumSet.of(ContextTag.LOYALTY));
	}

	public static void onDamaged(ServerPlayer player) {
		onDamaged(player, null);
	}

	public static void onDamaged(ServerPlayer player, DamageSource source) {
		if (minecartComboDamage(player, source)) {
			AdaptiveCombatManager.interrupt(player, family -> family != WeaponFamily.BOW);
		} else {
			AdaptiveCombatManager.interrupt(player);
		}
		CrossbowFeatures.interrupt(player);
	}

	public static boolean isGuarded(Player player) {
		return AdaptiveCombatManager.buildingUninterruptible(player);
	}

	public static boolean autoCrit(Player player) {
		return AdaptiveCombatManager.isReady(player, WeaponFamily.SWORD, AdaptiveStyle.RITMO_CORTE)
				|| AdaptiveCombatManager.isReady(player, WeaponFamily.SWORD, AdaptiveStyle.RESPOSTA_LAMINA);
	}

	public static boolean autoCrit(Player player, LivingEntity target) {
		if (player instanceof ServerPlayer server && HybridCombatManager.claims(server, com.example.modifiedpvp.combat.hybrid.HybridAction.SWORD_HIT)) {
			return false;
		}
		if (AdaptiveCombatManager.isReady(player, WeaponFamily.SWORD, AdaptiveStyle.RESPOSTA_LAMINA)) {
			return true;
		}
		return AdaptiveCombatManager.isReady(player, WeaponFamily.SWORD, AdaptiveStyle.RITMO_CORTE);
	}

	public static boolean rapidBow(Player player) {
		return AdaptiveCombatManager.isReady(player, WeaponFamily.BOW);
	}

	public static boolean dualFlurry(Player player) {
		return AdaptiveCombatManager.isReady(player, WeaponFamily.DUAL_SWORD, AdaptiveStyle.PRESSAO_ALTERNADA)
				|| AdaptiveCombatManager.isReady(player, WeaponFamily.DUAL_SWORD, AdaptiveStyle.ALTERNANCIA_PERFEITA);
	}

	public static void consumeBowShot(ServerPlayer player) {
		AdaptiveStyle style = AdaptiveCombatManager.readyStyle(player, WeaponFamily.BOW);
		if (style == AdaptiveStyle.MIRA_PERFEITA
				&& AdaptiveCombatManager.tryConsume(player, WeaponFamily.BOW, AdaptiveStyle.MIRA_PERFEITA, CombatActionType.BOW_ARROW_HIT, null, true, false)) {
			ComboData data = edit(player);
			data.bowShotsLeft = 1;
			save(player, data);
		}
	}

	public static boolean hasAxeEmpower(Player player) {
		return AdaptiveCombatManager.isReady(player, WeaponFamily.AXE);
	}

	public static void beginHurt(ServerPlayer player) {
		HURTS.push(new HurtFrame(player));
	}

	public static boolean endHurt(ServerPlayer player) {
		HurtFrame frame = HURTS.peek();
		if (frame == null || frame.player != player) {
			return false;
		}
		HURTS.pop();
		return frame.fullyBlocked;
	}

	public static void onShieldBlock(ServerPlayer player, DamageSource source, float incoming, float blocked) {
		HurtFrame frame = HURTS.peek();
		if (frame != null && frame.player == player && blocked > 0.0F && blocked + 1.0E-3F >= incoming) {
			frame.fullyBlocked = true;
		}
		if (reflecting || blocked <= 0.0F) {
			return;
		}
		ItemStack shield = player.getItemBlockingWith();
		if (shield == null || shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) {
			return;
		}

		ComboData data = edit(player);
		boolean dirty = false;
		boolean empowered = data.shieldEmpowered;
		if (empowered) {
			data.shieldEmpowered = false;
			dirty = true;
		}
		if (data.shieldBlocks < CombatRules.SHIELD_BLOCK_GOAL) {
			data.shieldBlocks++;
			dirty = true;
			if (data.shieldBlocks >= CombatRules.SHIELD_BLOCK_GOAL) {
				grant(player, Style.SHIELD);
			} else {
				goal(player, Style.SHIELD, data.shieldBlocks, CombatRules.SHIELD_BLOCK_GOAL);
			}
		}
		if (unlocked(player, Style.SHIELD)) {
			CombatActionType type = CombatActionType.SHIELD_BLOCK;
			EnumSet<ContextTag> tags = EnumSet.noneOf(ContextTag.class);
			if (ShieldFeatures.timedBlock(player)) {
				type = CombatActionType.SHIELD_BLOCK_TIMED;
				tags.add(ContextTag.PERFECT_TIMING);
			}
			LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
			LivingEntity ally = ShieldFeatures.threatenedAlly(player, source);
			if (ally != null) {
				type = CombatActionType.SHIELD_INTERCEPT_ALLY;
				tags.add(ContextTag.ALLY_THREAT);
			}
			AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.SHIELD, type, attacker, true, true, type == CombatActionType.SHIELD_BLOCK_TIMED, 1, tags);
			HybridCombatManager.onShield(player, attacker, type == CombatActionType.SHIELD_BLOCK_TIMED);
			if (consumed == AdaptiveStyle.INTERCEPTACAO && attacker != null) {
				FinisherEffects.interrupt(player, attacker, AdaptiveCombatManager.config().shieldShockTicks());
				data.shieldEmpowered = true;
				empowered = true;
				dirty = true;
			} else if (consumed == AdaptiveStyle.GUARDA_PROTECAO || consumed == AdaptiveStyle.GUARDA_INABALAVEL) {
				data.shieldEmpowered = true;
				empowered = true;
				dirty = true;
			}
		}
		if (dirty) {
			save(player, data);
		}
		ShieldFeatures.onBlock(player, source, empowered);
	}

	public static void onMaceMelee(ServerPlayer player, LivingEntity target) {
		if (!(target instanceof Mob) && !(target instanceof Player)) {
			return;
		}
		ComboData data = edit(player);
		boolean empowered = data.maceEmpowered;
		boolean dirty = false;
		if (empowered) {
			data.maceEmpowered = false;
			dirty = true;
		}
		if (unlocked(player, Style.MACE)) {
			boolean smash = !player.onGround() && player.fallDistance >= 1.5F;
			boolean windBurst = smash && ModEnchantments.level(player.level(), HybridCombatManager.WIND_BURST, player.getMainHandItem()) > 0;
			HybridResult hybrid = HybridCombatManager.onMace(player, target, smash, windBurst);
			if (!hybrid.exclusive()) {
				AdaptiveCombatManager.record(player, WeaponFamily.MACE, CombatActionType.CHAIN_ENTITY_IMPACT, target, true, false, false, 1, EnumSet.noneOf(ContextTag.class));
			}
		}
		if (dirty) {
			save(player, data);
		}
		if (empowered) {
			gather(player, target.position());
		}
	}

	public static void onMaceChain(ServerPlayer player, Vec3 center) {
		onMaceChain(player, center, false);
	}

	public static void onMaceChain(ServerPlayer player, Vec3 center, boolean entityHit) {
		if (!hasCombatant(player, center) && !entityHit) {
			return;
		}
		ComboData data = edit(player);
		boolean dirty = false;
		if (data.chainHits < CombatRules.MACE_CHAIN_GOAL) {
			data.chainHits++;
			dirty = true;
			if (data.chainHits >= CombatRules.MACE_CHAIN_GOAL) {
				grant(player, Style.MACE);
			} else {
				goal(player, Style.MACE, data.chainHits, CombatRules.MACE_CHAIN_GOAL);
			}
		}
		if (unlocked(player, Style.MACE)) {
			CombatActionType type = entityHit ? CombatActionType.CHAIN_ENTITY_IMPACT : CombatActionType.CHAIN_BLOCK_IMPACT;
			HybridCombatManager.onWind(player, false);
			AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.MACE, type, null, true, false, false, 1, EnumSet.noneOf(ContextTag.class));
			if (consumed == AdaptiveStyle.CONTROLE_IMPACTO) {
				data.maceEmpowered = true;
				dirty = true;
			} else if (consumed == AdaptiveStyle.PRECISAO_CORRENTE) {
				data.maceEmpowered = true;
				dirty = true;
			}
		}
		if (dirty) {
			save(player, data);
		}
		if (data.maceEmpowered && !entityHit) {
			gather(player, center);
		}
	}

	public static void onMaceReturn(ServerPlayer player) {
		if (unlocked(player, Style.MACE)) {
			AdaptiveCombatManager.record(player, WeaponFamily.MACE, CombatActionType.CHAIN_RETURN, null, true, false, false, 1, EnumSet.noneOf(ContextTag.class));
		}
	}

	public static boolean maceEmpowered(Player player) {
		ComboData data = player.getAttached(ATTACHMENT);
		return data != null && data.maceEmpowered;
	}

	public static int maceReturnCooldown(ServerPlayer player) {
		if (AdaptiveCombatManager.isReady(player, WeaponFamily.MACE, AdaptiveStyle.PRECISAO_CORRENTE) || maceEmpowered(player)) {
			return AdaptiveCombatManager.config().maceReturnCooldown();
		}
		return CombatRules.MACE_CHAIN_COOLDOWN;
	}

	public static double maceShockwaveScale(ServerPlayer player) {
		if (AdaptiveCombatManager.readyStyle(player, WeaponFamily.MACE) == AdaptiveStyle.CONTROLE_IMPACTO || maceEmpowered(player)) {
			return AdaptiveCombatManager.config().maceRepulseStrength();
		}
		return 1.0;
	}

	public static void consumeAxeEmpower(ServerPlayer player) {
		ComboData data = player.getAttached(ATTACHMENT);
		if (data == null || !data.axeEmpowered) {
			return;
		}
		data = data.copy();
		data.axeEmpowered = false;
		save(player, data);
	}

	public static void stun(LivingEntity target, Entity source, int ticks) {
		STUNS.removeIf(stun -> stun.target == target);
		STUNS.add(new Stun(target, ticks));
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4), source);
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1), source);
		target.setSprinting(false);
		target.setDeltaMovement(target.getDeltaMovement().multiply(0.05, 1.0, 0.05));
		target.hurtMarked = true;
		if (target instanceof Mob mob) {
			mob.getNavigation().stop();
		}
	}

	public static boolean isStunned(LivingEntity entity) {
		for (Stun stun : STUNS) {
			if (stun.target == entity && stun.ticks > 0) {
				return true;
			}
		}
		return false;
	}

	private static void onDeath(LivingEntity victim, DamageSource source) {
		if (victim instanceof ServerPlayer deadPlayer) {
			clearCombat(deadPlayer);
			AdaptiveCombatManager.clear(deadPlayer);
			HybridCombatManager.clear(deadPlayer);
			ControlGuard.forget(deadPlayer.getUUID());
		}
		if (!(victim instanceof Mob) || !(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (!killedWithSword(player, source)) {
			return;
		}
		if (WeaponStats.isDualWielding(player)) {
			addKills(player, Style.DUAL);
		} else {
			addKills(player, Style.SWORD);
		}
	}

	private static boolean killedWithSword(ServerPlayer player, DamageSource source) {
		ItemStack weapon = source.getWeaponItem();
		if (weapon != null && WeaponStats.isSword(weapon)) {
			return true;
		}
		return source.getDirectEntity() == player && WeaponStats.isSword(player.getMainHandItem());
	}

	private static void addKills(ServerPlayer player, Style style) {
		int goal = style == Style.DUAL ? CombatRules.DUAL_KILL_GOAL : CombatRules.SWORD_KILL_GOAL;
		ComboData data = edit(player);
		int kills = style == Style.DUAL ? data.dualKills : data.swordKills;
		if (kills >= goal) {
			return;
		}
		kills++;
		if (style == Style.DUAL) {
			data.dualKills = kills;
		} else {
			data.swordKills = kills;
		}
		save(player, data);
		if (kills >= goal) {
			grant(player, style);
		} else {
			goal(player, style, kills, goal);
		}
	}

	private static void advance(ServerPlayer player, Style style, LivingEntity focus) {
		if (!unlocked(player, style)) {
			return;
		}
		ComboData data = edit(player);
		if (pushCombo(player, data, style, focus)) {
			save(player, data);
		}
	}

	private static void advanceSword(ServerPlayer player, int action) {
		if (!unlocked(player, Style.SWORD)) {
			return;
		}
		ComboData data = edit(player);
		long now = player.level().getGameTime();
		if (now >= data.swordWindowUntil) {
			data.swordVariety = 0;
		}
		data.swordVariety |= action;
		data.swordLastAction = action;
		if (pushCombo(player, data, Style.SWORD, null)) {
			save(player, data);
		}
	}

	private static void advanceDual(ServerPlayer player, int hand) {
		if (!unlocked(player, Style.DUAL)) {
			return;
		}
		ComboData data = edit(player);
		long now = player.level().getGameTime();
		if (now >= data.dualWindowUntil) {
			data.dualLastHand = 0;
		}
		if (data.dualLastHand == hand && data.dualCombo > 0 && now < data.dualWindowUntil) {
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.variety"), true);
			return;
		}
		data.dualLastHand = hand;
		if (pushCombo(player, data, Style.DUAL, null)) {
			save(player, data);
		}
	}

	private static void clearCombat(ServerPlayer player) {
		CART_COMBO_GRACE.remove(player.getUUID());
		ComboData data = player.getAttached(ATTACHMENT);
		if (data == null) {
			return;
		}
		data = data.copy();
		data.clearCombat();
		save(player, data);
	}

	private static boolean pushCombo(ServerPlayer player, ComboData data, Style style, LivingEntity focus) {
		long now = player.level().getGameTime();
		long cooldownUntil = cooldownUntil(data, style);
		if (now < cooldownUntil) {
			int seconds = (int) Math.ceil((cooldownUntil - now) / 20.0);
			player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.cooldown", Component.translatable(style.nameKey()), seconds), true);
			return false;
		}

		int count = combo(data, style);
		long windowUntil = windowUntil(data, style);
		count = now < windowUntil ? count + 1 : 1;
		if (count >= style.hits()) {
			if (style == Style.SWORD && Integer.bitCount(data.swordVariety) < 2) {
				setCombo(data, style, count - 1);
				setWindow(data, style, now + style.window());
				player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.variety"), true);
				return true;
			}
			setCombo(data, style, 0);
			setWindow(data, style, 0L);
			setCooldown(data, style, now + style.cooldown());
			activate(player, data, style, focus, now);
			return true;
		}
		setCombo(data, style, count);
		setWindow(data, style, now + style.window());
		player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.progress", Component.translatable(style.nameKey()), count, style.hits()), true);
		return true;
	}

	private static void activate(ServerPlayer player, ComboData data, Style style, LivingEntity focus, long now) {
		switch (style) {
			case SWORD -> {
				data.swordBuffUntil = now + CombatRules.SWORD_EXECUTE_TICKS;
				data.swordVariety = 0;
				data.swordLastAction = 0;
			}
			case DUAL -> {
				data.dualBuffUntil = now + CombatRules.DUAL_FLURRY_TICKS;
				data.dualLastHand = 0;
			}
			case AXE -> data.axeEmpowered = true;
			case BOW -> {
				data.bowBuffUntil = now + CombatRules.BOW_BUFF_TICKS;
				data.bowShotsLeft = CombatRules.BOW_PRECISION_SHOTS;
			}
			case SHIELD -> data.shieldEmpowered = true;
			case MACE -> data.maceEmpowered = true;
			case TRIDENT -> {
				if (focus instanceof Player) {
					ControlGuard.offBalance(focus, player, CombatRules.TRIDENT_PVP_EXPOSE_TICKS);
					ControlGuard.exposeGuard(focus, player, CombatRules.TRIDENT_PVP_EXPOSE_TICKS);
				} else if (focus != null) {
					PENDING_SLOWS.add(new PendingSlow(focus, player));
				}
			}
		}
		Component special = style == Style.TRIDENT
				? Component.translatable(style.specialKey(), focus == null ? Component.translatable(style.nameKey()) : focus.getName())
				: Component.translatable(style.specialKey());
		player.displayClientMessage(special, true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9F, 0.75F);
		if (player.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(0.7), player.getZ(), 16, 0.4, 0.4, 0.4, 0.1);
		}
	}

	public static void rememberCrown(ServerPlayer player, HybridStyleId style) {
		int bit = switch (style) {
			case CROWN_OPENING -> 1;
			case KING_PRESSURE -> 2;
			case LAST_WORD -> 4;
			default -> 0;
		};
		if (bit == 0) {
			return;
		}
		ComboData data = edit(player);
		int next = data.crownMask | bit;
		if (next == data.crownMask) {
			return;
		}
		data.crownMask = next;
		save(player, data);
		if ((next & 7) == 7) {
			var server = player.level().getServer();
			if (server == null) {
				return;
			}
			AdvancementHolder holder = server.getAdvancements().get(ModifiedPvP.id("combat/crown"));
			if (holder != null) {
				player.getAdvancements().award(holder, "unlock");
			}
		}
	}

	private static void grant(ServerPlayer player, Style style) {
		if (unlocked(player, style)) {
			return;
		}
		var server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder holder = server.getAdvancements().get(ModifiedPvP.id(style.path()));
		if (holder != null) {
			player.getAdvancements().award(holder, "unlock");
		}
	}

	public static boolean unlockedFamily(ServerPlayer player, WeaponFamily family) {
		if (family == WeaponFamily.CROSSBOW) {
			return unlocked(player, Style.BOW);
		}
		if (family == WeaponFamily.SPEAR) {
			return unlocked(player, "combat/spear");
		}
		Style style = switch (family) {
			case SWORD -> Style.SWORD;
			case DUAL_SWORD -> Style.DUAL;
			case AXE -> Style.AXE;
			case BOW -> Style.BOW;
			case TRIDENT -> Style.TRIDENT;
			case SHIELD -> Style.SHIELD;
			case MACE -> Style.MACE;
			default -> null;
		};
		return style != null && unlocked(player, style);
	}

	public static void storeFamilyCooldown(ServerPlayer player, WeaponFamily family, long until) {
		ComboData data = edit(player);
		setFamilyCooldown(data, family, until);
		save(player, data);
	}

	public static void storeFamilyCooldowns(ServerPlayer player, AdaptiveCombatState state) {
		ComboData data = edit(player);
		for (WeaponFamily family : WeaponFamily.values()) {
			setFamilyCooldown(data, family, state.family(family).cooldownUntil());
		}
		save(player, data);
	}

	public static void loadFamilyCooldowns(ServerPlayer player, AdaptiveCombatState state) {
		ComboData data = player.getAttached(ATTACHMENT);
		if (data == null) {
			return;
		}
		for (WeaponFamily family : WeaponFamily.values()) {
			FamilyCombatState familyState = state.family(family);
			long until = familyCooldown(data, family);
			familyState.setCooldownUntil(until);
			if (until > 0L) {
				familyState.setPhase(FamilyPhase.COOLDOWN);
			}
		}
	}

	public static void onHeavyResolved(ServerPlayer player, LivingEntity target, boolean blocked, boolean hit) {
		if (!unlocked(player, Style.AXE)) {
			return;
		}
		CombatActionType type = blocked ? CombatActionType.HEAVY_BLOCKED : CombatActionType.HEAVY_FULL_HIT;
		AdaptiveCombatManager.record(player, WeaponFamily.AXE, type, target, hit, blocked, false, hit ? 1 : 0, EnumSet.noneOf(ContextTag.class));
	}

	public static void onBowCartIgnition(ServerPlayer player) {
		CART_COMBO_GRACE.put(player.getUUID(), player.level().getGameTime() + CART_COMBO_GRACE_TICKS);
		if (unlocked(player, Style.BOW)) {
			AdaptiveCombatManager.record(player, WeaponFamily.BOW, CombatActionType.BOW_CART_IGNITION, null, true, false, false, 1, EnumSet.of(ContextTag.CART_IGNITION));
		}
	}

	private static boolean minecartComboDamage(ServerPlayer player, DamageSource source) {
		Long grace = CART_COMBO_GRACE.get(player.getUUID());
		boolean recentCartHit = grace != null && player.level().getGameTime() < grace;
		if (source == null) {
			return recentCartHit;
		}
		Entity entity = source.getDirectEntity() != null ? source.getDirectEntity() : source.getEntity();
		if (entity != null && entity.getType() == EntityType.TNT_MINECART) {
			return true;
		}
		return recentCartHit && source.is(DamageTypeTags.IS_EXPLOSION);
	}

	public static void onFireworkSplash(ServerPlayer player, LivingEntity focus, int affected, boolean grapple) {
		if (grapple || affected <= 0) {
			return;
		}
		AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.CROSSBOW, CombatActionType.CROSSBOW_FIREWORK_SPLASH, focus, true, false, false, affected, EnumSet.noneOf(ContextTag.class));
		if (consumed == null || focus == null) {
			return;
		}
		double strength = consumed == AdaptiveStyle.PRECISAO_EXPLOSIVA
				? AdaptiveCombatManager.config().crossbowFocus()
				: AdaptiveCombatManager.config().crossbowScatter();
		FinisherEffects.shove(player, focus, strength, AdaptiveCombatManager.config());
	}

	public static void onTridentThrowHit(ServerPlayer player, LivingEntity target, boolean channeling) {
		if (target instanceof Drowned) {
			grant(player, Style.TRIDENT);
		}
		EnumSet<ContextTag> tags = EnumSet.noneOf(ContextTag.class);
		CombatActionType type = CombatActionType.TRIDENT_THROW_HIT;
		if (channeling) {
			tags.add(ContextTag.CHANNELING_VALID);
			type = CombatActionType.CHANNELING_HIT;
		}
		AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.TRIDENT, type, target, true, false, false, 1, tags);
		if (consumed == AdaptiveStyle.DOMINIO_TEMPESTADE && target != null && !channeling) {
			FinisherEffects.shove(player, target, AdaptiveCombatManager.config().tridentShove(), AdaptiveCombatManager.config());
		}
	}

	public static void onRiptideHit(ServerPlayer player, LivingEntity target) {
		AdaptiveStyle consumed = AdaptiveCombatManager.record(player, WeaponFamily.TRIDENT, CombatActionType.RIPTIDE_HIT, target, true, false, false, 1, EnumSet.of(ContextTag.RIPTIDE, ContextTag.WET));
		if (consumed == AdaptiveStyle.DOMINIO_MARES && target != null) {
			FinisherEffects.shove(player, target, AdaptiveCombatManager.config().tridentShove(), AdaptiveCombatManager.config());
		}
	}

	private static long familyCooldown(ComboData data, WeaponFamily family) {
		return switch (family) {
			case SWORD -> data.swordCooldownUntil;
			case DUAL_SWORD -> data.dualCooldownUntil;
			case AXE -> data.axeCooldownUntil;
			case BOW -> data.bowCooldownUntil;
			case TRIDENT -> data.tridentCooldownUntil;
			case SHIELD -> data.shieldCooldownUntil;
			case MACE -> data.maceCooldownUntil;
			case CROSSBOW -> data.crossbowCooldownUntil;
			case SPEAR -> data.spearCooldownUntil;
		};
	}

	private static void setFamilyCooldown(ComboData data, WeaponFamily family, long until) {
		switch (family) {
			case SWORD -> data.swordCooldownUntil = until;
			case DUAL_SWORD -> data.dualCooldownUntil = until;
			case AXE -> data.axeCooldownUntil = until;
			case BOW -> data.bowCooldownUntil = until;
			case TRIDENT -> data.tridentCooldownUntil = until;
			case SHIELD -> data.shieldCooldownUntil = until;
			case MACE -> data.maceCooldownUntil = until;
			case CROSSBOW -> data.crossbowCooldownUntil = until;
			case SPEAR -> data.spearCooldownUntil = until;
		}
	}

	private static boolean unlocked(ServerPlayer player, String path) {
		if (ModGameRules.unlockSpecials(player)) {
			return true;
		}
		var server = player.level().getServer();
		if (server == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(ModifiedPvP.id(path));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static boolean unlocked(ServerPlayer player, Style style) {
		if (ModGameRules.unlockSpecials(player)) {
			return true;
		}
		var server = player.level().getServer();
		if (server == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(ModifiedPvP.id(style.path()));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void goal(ServerPlayer player, Style style, int current, int goal) {
		player.displayClientMessage(Component.translatable("combat.modifiedpvp.combo.goal", Component.translatable(style.nameKey()), current, goal), true);
	}

	private static ComboData edit(ServerPlayer player) {
		ComboData current = player.getAttached(ATTACHMENT);
		return current == null ? new ComboData() : current.copy();
	}

	private static void save(ServerPlayer player, ComboData data) {
		player.setAttached(ATTACHMENT, data);
	}

	private static boolean active(int count, long until, long now) {
		return count > 0 && now < until;
	}

	private static int combo(ComboData data, Style style) {
		return switch (style) {
			case SWORD -> data.swordCombo;
			case DUAL -> data.dualCombo;
			case AXE -> data.axeCombo;
			case BOW -> data.bowCombo;
			case TRIDENT -> data.tridentCombo;
			case SHIELD -> data.shieldCombo;
			case MACE -> data.maceCombo;
		};
	}

	private static void setCombo(ComboData data, Style style, int count) {
		switch (style) {
			case SWORD -> data.swordCombo = count;
			case DUAL -> data.dualCombo = count;
			case AXE -> data.axeCombo = count;
			case BOW -> data.bowCombo = count;
			case TRIDENT -> data.tridentCombo = count;
			case SHIELD -> data.shieldCombo = count;
			case MACE -> data.maceCombo = count;
		}
	}

	private static long windowUntil(ComboData data, Style style) {
		return switch (style) {
			case SWORD -> data.swordWindowUntil;
			case DUAL -> data.dualWindowUntil;
			case AXE -> data.axeWindowUntil;
			case BOW -> data.bowWindowUntil;
			case TRIDENT -> data.tridentWindowUntil;
			case SHIELD -> data.shieldWindowUntil;
			case MACE -> data.maceWindowUntil;
		};
	}

	private static void setWindow(ComboData data, Style style, long until) {
		switch (style) {
			case SWORD -> data.swordWindowUntil = until;
			case DUAL -> data.dualWindowUntil = until;
			case AXE -> data.axeWindowUntil = until;
			case BOW -> data.bowWindowUntil = until;
			case TRIDENT -> data.tridentWindowUntil = until;
			case SHIELD -> data.shieldWindowUntil = until;
			case MACE -> data.maceWindowUntil = until;
		}
	}

	private static long cooldownUntil(ComboData data, Style style) {
		return switch (style) {
			case SWORD -> data.swordCooldownUntil;
			case DUAL -> data.dualCooldownUntil;
			case AXE -> data.axeCooldownUntil;
			case BOW -> data.bowCooldownUntil;
			case TRIDENT -> data.tridentCooldownUntil;
			case SHIELD -> data.shieldCooldownUntil;
			case MACE -> data.maceCooldownUntil;
		};
	}

	private static void setCooldown(ComboData data, Style style, long until) {
		switch (style) {
			case SWORD -> data.swordCooldownUntil = until;
			case DUAL -> data.dualCooldownUntil = until;
			case AXE -> data.axeCooldownUntil = until;
			case BOW -> data.bowCooldownUntil = until;
			case TRIDENT -> data.tridentCooldownUntil = until;
			case SHIELD -> data.shieldCooldownUntil = until;
			case MACE -> data.maceCooldownUntil = until;
		}
	}

	private static void tickSlows() {
		Iterator<PendingSlow> iterator = PENDING_SLOWS.iterator();
		while (iterator.hasNext()) {
			PendingSlow pending = iterator.next();
			LivingEntity target = pending.target;
			if (!target.isAlive() || target.isRemoved()) {
				iterator.remove();
				continue;
			}
			if (pending.wait > 0) {
				pending.wait--;
				continue;
			}
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, CombatRules.TRIDENT_SLOW_TICKS, CombatRules.TRIDENT_SLOW_AMPLIFIER), pending.source);
			iterator.remove();
		}
	}

	private static void tickStuns() {
		Iterator<Stun> iterator = STUNS.iterator();
		while (iterator.hasNext()) {
			Stun stun = iterator.next();
			if (!stun.target.isAlive() || stun.ticks <= 0) {
				iterator.remove();
				continue;
			}
			stun.target.setDeltaMovement(stun.target.getDeltaMovement().multiply(0.2, 1.0, 0.2));
			stun.target.hurtMarked = true;
			stun.target.setSprinting(false);
			if (stun.target instanceof Mob mob) {
				mob.getNavigation().stop();
			}
			stun.ticks--;
		}
	}

	private enum Style {
		SWORD("combat/sword", "combat.modifiedpvp.combo.sword", "combat.modifiedpvp.combo.sword_special", CombatRules.SWORD_COMBO_HITS, CombatRules.COMBO_WINDOW_SWORD, CombatRules.SWORD_COMBO_COOLDOWN),
		DUAL("combat/dual", "combat.modifiedpvp.combo.dual", "combat.modifiedpvp.combo.dual_special", CombatRules.DUAL_COMBO_HITS, CombatRules.COMBO_WINDOW_DUAL, CombatRules.DUAL_COMBO_COOLDOWN),
		AXE("combat/axe", "combat.modifiedpvp.combo.axe", "combat.modifiedpvp.combo.axe_special", CombatRules.AXE_COMBO_HITS, CombatRules.COMBO_WINDOW_AXE, CombatRules.AXE_COMBO_COOLDOWN),
		BOW("combat/bow", "combat.modifiedpvp.combo.bow", "combat.modifiedpvp.combo.bow_special", CombatRules.BOW_COMBO_HITS, CombatRules.COMBO_WINDOW_BOW, CombatRules.BOW_COMBO_COOLDOWN),
		TRIDENT("combat/trident", "combat.modifiedpvp.combo.trident", "combat.modifiedpvp.combo.trident_special", CombatRules.TRIDENT_COMBO_HITS, CombatRules.COMBO_WINDOW_TRIDENT, CombatRules.TRIDENT_COMBO_COOLDOWN),
		SHIELD("combat/shield", "combat.modifiedpvp.combo.shield", "combat.modifiedpvp.combo.shield_special", CombatRules.SHIELD_COMBO_BLOCKS, CombatRules.COMBO_WINDOW_SHIELD, CombatRules.SHIELD_COMBO_COOLDOWN),
		MACE("combat/mace", "combat.modifiedpvp.combo.mace", "combat.modifiedpvp.combo.mace_special", CombatRules.MACE_COMBO_HITS, CombatRules.COMBO_WINDOW_MACE, CombatRules.MACE_COMBO_COOLDOWN);

		private final String path;
		private final String nameKey;
		private final String specialKey;
		private final int hits;
		private final int window;
		private final int cooldown;

		Style(String path, String nameKey, String specialKey, int hits, int window, int cooldown) {
			this.path = path;
			this.nameKey = nameKey;
			this.specialKey = specialKey;
			this.hits = hits;
			this.window = window;
			this.cooldown = cooldown;
		}

		private String path() {
			return this.path;
		}

		private String nameKey() {
			return this.nameKey;
		}

		private String specialKey() {
			return this.specialKey;
		}

		private int hits() {
			return this.hits;
		}

		private int window() {
			return this.window;
		}

		private int cooldown() {
			return this.cooldown;
		}
	}

	private static void gather(ServerPlayer player, Vec3 center) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		double radius = CombatRules.MACE_SHOCKWAVE_RADIUS;
		AABB area = new AABB(center, center).inflate(radius);
		for (Entity entity : level.getEntities(player, area, candidate -> combatant(player, candidate))) {
			LivingEntity living = (LivingEntity) entity;
			if (living.distanceToSqr(center) > radius * radius) {
				continue;
			}
			Vec3 flat = new Vec3(center.x - living.getX(), 0.0, center.z - living.getZ());
			if (flat.lengthSqr() > 0.12) {
				Vec3 pull = flat.normalize().scale(CombatRules.MACE_PULL_STRENGTH);
				living.setDeltaMovement(pull.x, Math.max(living.getDeltaMovement().y, 0.2), pull.z);
				living.setOnGround(false);
				living.hurtMarked = true;
			}
			if (!(living instanceof Player)) {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, CombatRules.MACE_SLOW_TICKS, CombatRules.MACE_SLOW_AMPLIFIER), player);
			}
		}
		level.playSound(null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.9F, 0.8F);
		level.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.2, center.z, 18, 0.6, 0.2, 0.6, 0.02);
	}

	private static boolean hasCombatant(ServerPlayer player, Vec3 center) {
		if (!(player.level() instanceof ServerLevel level)) {
			return false;
		}
		double radius = CombatRules.MACE_SHOCKWAVE_RADIUS;
		AABB area = new AABB(center, center).inflate(radius);
		for (Entity entity : level.getEntities(player, area, candidate -> combatant(player, candidate))) {
			if (entity.distanceToSqr(center) <= radius * radius) {
				return true;
			}
		}
		return false;
	}

	private static boolean combatant(Player attacker, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator() || living == attacker || !living.attackable()) {
			return false;
		}
		if (!(living instanceof Mob) && !(living instanceof Player)) {
			return false;
		}
		if (living instanceof Player other) {
			return !other.getAbilities().invulnerable && attacker.canHarmPlayer(other);
		}
		return true;
	}

	private static final class HurtFrame {
		private final ServerPlayer player;
		private boolean fullyBlocked;

		private HurtFrame(ServerPlayer player) {
			this.player = player;
		}
	}

	private static final class PendingSlow {
		private final LivingEntity target;
		private final Entity source;
		private int wait;

		private PendingSlow(LivingEntity target, Entity source) {
			this.target = target;
			this.source = source;
			MobEffectInstance current = target.getEffect(MobEffects.SLOWNESS);
			this.wait = current != null && current.getAmplifier() > CombatRules.TRIDENT_SLOW_AMPLIFIER ? current.getDuration() : 0;
		}
	}

	private static final class Stun {
		private final LivingEntity target;
		private int ticks;

		private Stun(LivingEntity target, int ticks) {
			this.target = target;
			this.ticks = ticks;
		}
	}
}
