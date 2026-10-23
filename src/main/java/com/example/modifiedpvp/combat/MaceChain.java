package com.example.modifiedpvp.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.modifiedpvp.mixin.BlockDisplayInvoker;
import com.example.modifiedpvp.mixin.DisplayInvoker;
import com.mojang.math.Transformation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MaceChain {
	private static final String DISPLAY_TAG = "modifiedpvp.mace_chain";
	private static final int LINK_COUNT = 8;
	private static final int VISUAL_INTERVAL = 2;
	private static final float VIEW_RANGE = 0.6F;
	private static final double HIT_MARGIN = 0.3;
	private static final double MACE_LENGTH = 1.05;

	private static final Map<UUID, Throw> THROWS = new HashMap<>();
	private static final Set<Entity> LIVE_DISPLAYS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final List<Entity> ORPHANS = new ArrayList<>();

	private MaceChain() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!canThrow(player, hand)) {
				return InteractionResult.PASS;
			}
			if (player instanceof ServerPlayer serverPlayer && !tryThrow(serverPlayer)) {
				return InteractionResult.PASS;
			}
			return InteractionResult.SUCCESS;
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player == null) {
				return;
			}
			Throw active = THROWS.remove(handler.player.getUUID());
			if (active != null) {
				giveBack(active);
				clearDisplays(active);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Throw active : THROWS.values()) {
				giveBack(active);
				clearDisplays(active);
			}
			THROWS.clear();
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.getTags().contains(DISPLAY_TAG) && !LIVE_DISPLAYS.contains(entity)) {
				ORPHANS.add(entity);
			}
		});
	}

	public static boolean canThrow(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return hand == InteractionHand.MAIN_HAND
				&& stack.is(Items.MACE)
				&& player.isAlive()
				&& !player.isSpectator()
				&& !player.getCooldowns().isOnCooldown(stack);
	}

	public static boolean tryThrow(ServerPlayer player) {
		if (!canThrow(player, InteractionHand.MAIN_HAND) || THROWS.containsKey(player.getUUID())) {
			return false;
		}
		ServerLevel level = player.level();
		ItemStack stack = player.getMainHandItem();
		int chain = ModEnchantments.level(level, ModEnchantments.CHAIN, stack);
		double range = CombatRules.MACE_CHAIN_BASE_RANGE + chain * CombatRules.MACE_CHAIN_RANGE_PER_LEVEL;
		int slot = player.getInventory().getSelectedSlot();
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		Vec3 look = player.getLookAngle().normalize();
		Vec3 start = anchor(player).add(look.scale(0.6));
		List<Display> model = spawnMaceModel(level, start, yaw(look), pitch(look));
		List<Display> links = spawnChain(level, anchor(player));
		THROWS.put(player.getUUID(), new Throw(player, level, stack, slot, model, links, start, look.scale(CombatRules.MACE_CHAIN_THROW_SPEED), range));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.9F, 0.6F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
		return true;
	}

	public static void tick() {
		for (Entity orphan : ORPHANS) {
			orphan.discard();
		}
		ORPHANS.clear();

		Iterator<Throw> iterator = THROWS.values().iterator();
		while (iterator.hasNext()) {
			Throw active = iterator.next();
			if (!advance(active)) {
				clearDisplays(active);
				iterator.remove();
			}
		}
	}

	private static boolean advance(Throw active) {
		ServerPlayer player = active.player;
		if (!player.isAlive()) {
			dropAtHead(active);
			return false;
		}
		if (player.isRemoved() || player.level() != active.level) {
			giveBack(active);
			return false;
		}

		active.ticks++;
		Vec3 anchor = anchor(player);
		if (!active.returning) {
			Vec3 next = active.pos.add(active.velocity);
			HitResult hit = findHit(active, next);
			if (hit != null) {
				active.pos = hit.getLocation();
				impact(active, hit);
				active.returning = true;
			} else {
				active.pos = next;
				active.velocity = active.velocity.add(0.0, -CombatRules.MACE_CHAIN_GRAVITY, 0.0);
				if (active.pos.distanceTo(anchor) >= active.range) {
					active.returning = true;
				}
			}
		} else {
			Vec3 toAnchor = anchor.subtract(active.pos);
			double distance = toAnchor.length();
			if (distance <= CombatRules.MACE_CHAIN_RETURN_SPEED) {
				giveBack(active);
				return false;
			}
			active.pos = active.pos.add(toAnchor.scale(CombatRules.MACE_CHAIN_RETURN_SPEED / distance));
		}
		if (active.stack.isEmpty()) {
			return false;
		}
		if (active.ticks > CombatRules.MACE_CHAIN_MAX_TICKS) {
			giveBack(active);
			return false;
		}
		if (active.ticks % VISUAL_INTERVAL == 0) {
			updateDisplays(active, anchor);
		}
		return true;
	}

	private static HitResult findHit(Throw active, Vec3 next) {
		ServerLevel level = active.level;
		BlockHitResult block = level.clip(new ClipContext(active.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, active.player));
		boolean blocked = block.getType() != HitResult.Type.MISS;
		Vec3 end = blocked ? block.getLocation() : next;

		EntityHitResult closest = null;
		double bestDistance = Double.MAX_VALUE;
		AABB sweep = new AABB(active.pos, end).inflate(1.0);
		for (Entity entity : level.getEntities(active.player, sweep, candidate -> canHit(active.player, candidate))) {
			Optional<Vec3> point = entity.getBoundingBox().inflate(HIT_MARGIN).clip(active.pos, end);
			if (point.isEmpty()) {
				continue;
			}
			double distance = active.pos.distanceToSqr(point.get());
			if (distance < bestDistance) {
				bestDistance = distance;
				closest = new EntityHitResult(entity, point.get());
			}
		}
		if (closest != null) {
			return closest;
		}
		return blocked ? block : null;
	}

	private static void impact(Throw active, HitResult hit) {
		ServerLevel level = active.level;
		ServerPlayer player = active.player;
		Vec3 center = hit.getLocation();
		if (hit instanceof BlockHitResult blockHit) {
			Direction face = blockHit.getDirection();
			center = center.add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3);
		}

		boolean entityHit = false;
		if (hit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity target) {
			entityHit = true;
			DamageSource source = level.damageSources().playerAttack(player);
			float damage = WeaponStats.attackDamage(active.stack) * CombatRules.MACE_CHAIN_DAMAGE_RATIO;
			damage = EnchantmentHelper.modifyDamage(level, active.stack, target, source, damage);
			if (target.hurtServer(level, source, damage)) {
				EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, active.stack);
			}
		}

		shockwave(level, player, center);
		ComboSpecials.onMaceChain(player, center, entityHit);
		active.stack.hurtAndBreak(1, level, player, item -> {
		});
		level.playSound(null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.0F, 1.1F);
		level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
		for (int i = 0; i < 20; i++) {
			double angle = Math.PI * 2.0 * i / 20.0;
			level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.1, center.z, 0, Math.cos(angle), 0.05, Math.sin(angle), 0.35);
		}
	}

	private static void shockwave(ServerLevel level, ServerPlayer player, Vec3 center) {
		double radius = CombatRules.MACE_SHOCKWAVE_RADIUS;
		Vec3 look = WeaponStats.horizontalLook(player);
		AABB area = new AABB(center, center).inflate(radius);
		for (Entity entity : level.getEntities((Entity) null, area, candidate -> candidate == player || canHit(player, candidate))) {
			LivingEntity living = (LivingEntity) entity;
			double distance = living.getBoundingBox().getCenter().distanceTo(center);
			if (distance > radius) {
				continue;
			}
			double falloff = 1.0 - (distance / radius) * 0.5;
			if (living == player) {
				launchThrower(player, center, falloff);
				continue;
			}
			double dx = center.x - living.getX();
			double dz = center.z - living.getZ();
			if (dx * dx + dz * dz < 1.0E-4) {
				dx = -look.x;
				dz = -look.z;
			}
			living.knockback(CombatRules.MACE_SHOCKWAVE_STRENGTH * falloff * ComboSpecials.maceShockwaveScale(player), dx, dz);
			Vec3 motion = living.getDeltaMovement();
			living.setDeltaMovement(motion.x, Math.max(motion.y, CombatRules.MACE_SHOCKWAVE_LIFT * falloff), motion.z);
			living.setOnGround(false);
			living.hurtMarked = true;
		}
	}

	private static void launchThrower(ServerPlayer player, Vec3 center, double falloff) {
		Vec3 away = player.getBoundingBox().getCenter().subtract(center);
		Vec3 direction = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 1.0, 0.0) : away.normalize();
		double strength = CombatRules.MACE_SHOCKWAVE_SELF_STRENGTH * falloff;
		Vec3 motion = player.getDeltaMovement().add(direction.scale(strength));
		player.setDeltaMovement(motion.x, Math.max(motion.y, CombatRules.MACE_SHOCKWAVE_LIFT * falloff), motion.z);
		player.setOnGround(false);
		player.hurtMarked = true;
		player.applyPostImpulseGraceTime(6);
	}

	private static void updateDisplays(Throw active, Vec3 anchor) {
		Vec3 span = active.pos.subtract(anchor);
		Vec3 direction = span.lengthSqr() > 1.0E-4 ? span.normalize() : active.velocity.normalize();
		float yaw = yaw(direction);
		float pitch = pitch(direction);
		for (Display part : active.model) {
			part.snapTo(active.pos.x, active.pos.y, active.pos.z, yaw, pitch);
		}

		Vec3 chainEnd = active.pos.subtract(direction.scale(MACE_LENGTH));
		Vec3 chain = chainEnd.subtract(anchor);
		int count = active.links.size();
		for (int i = 0; i < count; i++) {
			Vec3 point = anchor.add(chain.scale((i + 1) / (double) (count + 1)));
			active.links.get(i).snapTo(point.x, point.y, point.z, 0.0F, 0.0F);
		}
	}

	private static List<Display> spawnMaceModel(ServerLevel level, Vec3 pos, float yaw, float pitch) {
		List<Display> model = new ArrayList<>();
		model.add(spawnBlockPart(level, Blocks.HEAVY_CORE.defaultBlockState(), pos, yaw, pitch, new Vector3f(-0.5F, -0.25F, -0.5F), new Vector3f(1.0F)));
		model.add(spawnBlockPart(level, Blocks.STRIPPED_DARK_OAK_WOOD.defaultBlockState(), pos, yaw, pitch, new Vector3f(-0.06F, -0.06F, -1.0F), new Vector3f(0.12F, 0.12F, 0.8F)));
		return model;
	}

	private static List<Display> spawnChain(ServerLevel level, Vec3 pos) {
		List<Display> links = new ArrayList<>();
		for (int i = 0; i < LINK_COUNT; i++) {
			links.add(spawnChainLink(level, pos));
		}
		return links;
	}

	private static Display spawnBlockPart(ServerLevel level, BlockState state, Vec3 pos, float yaw, float pitch, Vector3f translation, Vector3f scale) {
		Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
		((BlockDisplayInvoker) display).modifiedpvp$setBlockState(state);
		return spawnDisplay(level, display, pos, yaw, pitch, Display.BillboardConstraints.FIXED, translation, scale);
	}

	private static Display spawnChainLink(ServerLevel level, Vec3 pos) {
		Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
		display.getSlot(0).set(new ItemStack(Items.IRON_CHAIN));
		return spawnDisplay(level, display, pos, 0.0F, 0.0F, Display.BillboardConstraints.CENTER, new Vector3f(), new Vector3f(0.45F));
	}

	private static Display spawnDisplay(ServerLevel level, Display display, Vec3 pos, float yaw, float pitch, Display.BillboardConstraints billboard, Vector3f translation, Vector3f scale) {
		DisplayInvoker invoker = (DisplayInvoker) display;
		invoker.modifiedpvp$setPosRotInterpolationDuration(VISUAL_INTERVAL);
		invoker.modifiedpvp$setViewRange(VIEW_RANGE);
		invoker.modifiedpvp$setBillboardConstraints(billboard);
		invoker.modifiedpvp$setTransformation(new Transformation(translation, new Quaternionf(), scale, new Quaternionf()));
		display.addTag(DISPLAY_TAG);
		display.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
		LIVE_DISPLAYS.add(display);
		level.addFreshEntity(display);
		return display;
	}

	private static void discardDisplay(Entity display) {
		LIVE_DISPLAYS.remove(display);
		display.discard();
	}

	private static void clearDisplays(Throw active) {
		for (Entity part : active.model) {
			discardDisplay(part);
		}
		for (Entity link : active.links) {
			discardDisplay(link);
		}
		active.links.clear();
	}

	private static void giveBack(Throw active) {
		if (active.stack.isEmpty()) {
			return;
		}
		ServerPlayer player = active.player;
		ComboSpecials.onMaceReturn(player);
		player.getCooldowns().addCooldown(active.stack, ComboSpecials.maceReturnCooldown(player));
		Inventory inventory = player.getInventory();
		if (inventory.getItem(active.slot).isEmpty()) {
			inventory.setItem(active.slot, active.stack);
		} else if (!inventory.add(active.stack)) {
			player.drop(active.stack, false);
		}
		active.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 1.0F);
	}

	private static void dropAtHead(Throw active) {
		if (active.stack.isEmpty()) {
			return;
		}
		ItemEntity item = new ItemEntity(active.level, active.pos.x, active.pos.y, active.pos.z, active.stack);
		item.setDefaultPickUpDelay();
		active.level.addFreshEntity(item);
	}

	private static Vec3 anchor(Player player) {
		return player.getEyePosition().add(0.0, -0.4, 0.0);
	}

	private static float yaw(Vec3 direction) {
		return (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
	}

	private static float pitch(Vec3 direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		return (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
	}

	private static boolean canHit(Player attacker, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator() || living == attacker || !living.attackable()) {
			return false;
		}
		if (living instanceof Player other) {
			return !other.getAbilities().invulnerable && attacker.canHarmPlayer(other);
		}
		return true;
	}

	private static final class Throw {
		private final ServerPlayer player;
		private final ServerLevel level;
		private final ItemStack stack;
		private final int slot;
		private final List<Display> model;
		private final List<Display> links;
		private final double range;
		private Vec3 pos;
		private Vec3 velocity;
		private boolean returning;
		private int ticks;

		private Throw(ServerPlayer player, ServerLevel level, ItemStack stack, int slot, List<Display> model, List<Display> links, Vec3 pos, Vec3 velocity, double range) {
			this.player = player;
			this.level = level;
			this.stack = stack;
			this.slot = slot;
			this.model = model;
			this.links = links;
			this.pos = pos;
			this.velocity = velocity;
			this.range = range;
		}
	}
}
