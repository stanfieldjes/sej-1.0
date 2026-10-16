package dev.sej;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One spawn egg for every mob. A fresh egg opens the mob menu on right-click; once a mob is
 * picked, right-click a block to spawn it, water to spawn it in the water, or a monster
 * spawner to set what it spawns. Sneak + right-click opens the menu again to change the mob.
 */
public class MobSpawnEggItem extends Item {

    /** Mobs in the menu, in order. Left column (animals) is the first 6, right column (monsters) the rest. */
    public static final List<EntityType<?>> ANIMALS = List.of(
            EntityType.PIG, EntityType.SHEEP, EntityType.COW, EntityType.CHICKEN, EntityType.SQUID, EntityType.WOLF);
    public static final List<EntityType<?>> MONSTERS = List.of(
            EntityType.CREEPER, EntityType.SKELETON, EntityType.SPIDER, EntityType.ZOMBIE, EntityType.SLIME);

    /** Client -> server: the player picked a mob for the egg in their hand. */
    public static final Identifier SELECT_MOB = new Identifier("bettabeta", "select_mob");

    private static final String MOB_KEY = "BettaBetaMob";

    /** Set by the client entrypoint; opens the mob menu. Does nothing on a dedicated server. */
    public static Consumer<Hand> menuOpener = hand -> {};

    public static final MobSpawnEggItem ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(Sej.MOD_ID, "spawn_egg"),
            new MobSpawnEggItem(new Item.Settings()));

    public MobSpawnEggItem(Settings settings) {
        super(settings);
    }

    /** Call from onInitialize: registers the item and the menu's packet. */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SELECT_MOB, (server, player, handler, buf, responseSender) -> {
            Hand hand = buf.readEnumConstant(Hand.class);
            Identifier id = buf.readIdentifier();
            server.execute(() -> {
                EntityType<?> type = Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
                if (!isAllowed(type)) return;
                ItemStack stack = player.getStackInHand(hand);
                if (stack.isOf(ITEM)) setMob(stack, type);
            });
        });
    }

    // ---------------------------------------------------------------- the stored mob

    public static boolean isAllowed(@Nullable EntityType<?> type) {
        return type != null && (ANIMALS.contains(type) || MONSTERS.contains(type));
    }

    @Nullable
    public static EntityType<?> getMob(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(MOB_KEY, NbtElement.STRING_TYPE)) return null;
        Identifier id = Identifier.tryParse(nbt.getString(MOB_KEY));
        EntityType<?> type = id == null ? null : Registries.ENTITY_TYPE.getOrEmpty(id).orElse(null);
        return isAllowed(type) ? type : null;
    }

    public static void setMob(ItemStack stack, EntityType<?> type) {
        stack.getOrCreateNbt().putString(MOB_KEY, Registries.ENTITY_TYPE.getId(type).toString());
    }

    @Override
    public Text getName(ItemStack stack) {
        EntityType<?> type = getMob(stack);
        return type == null
                ? super.getName(stack)
                : Text.translatable("item.sej.spawn_egg.mob", type.getName());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(getMob(stack) == null
                ? "item.sej.spawn_egg.hint_pick"
                : "item.sej.spawn_egg.hint_change").formatted(Formatting.GRAY));
    }

    // ---------------------------------------------------------------- using it

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        EntityType<?> type = getMob(stack);

        if (type == null || (player != null && player.isSneaking())) {
            if (world.isClient) menuOpener.accept(context.getHand());
            return ActionResult.success(world.isClient);
        }
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        BlockState state = world.getBlockState(pos);

        // Monster spawner: set what it spawns, like vanilla eggs.
        if (state.isOf(Blocks.SPAWNER) && world.getBlockEntity(pos) instanceof MobSpawnerBlockEntity spawner) {
            spawner.setEntityType(type, world.getRandom());
            spawner.markDirty();
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
            world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            consume(player, stack);
            return ActionResult.CONSUME;
        }

        BlockPos spawnPos = state.getCollisionShape(world, pos).isEmpty() ? pos : pos.offset(side);
        Entity spawned = type.spawnFromItemStack((ServerWorld) world, stack, player, spawnPos,
                SpawnReason.SPAWN_EGG, true, !Objects.equals(pos, spawnPos) && side == Direction.UP);
        if (spawned != null) {
            consume(player, stack);
            world.emitGameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
        return ActionResult.CONSUME;
    }

    /**
     * Right-clicking without aiming at a block: spawns into water (for squid) once a mob is picked.
     * The menu only opens here for a fresh egg, or on sneak + right-click.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        EntityType<?> type = getMob(stack);

        if (type == null || player.isSneaking()) {
            if (world.isClient) menuOpener.accept(hand);
            return TypedActionResult.success(stack, world.isClient);
        }

        BlockHitResult hit = raycast(world, player, RaycastContext.FluidHandling.SOURCE_ONLY);
        BlockPos pos = hit.getBlockPos();
        if (hit.getType() != HitResult.Type.BLOCK || !(world.getBlockState(pos).getBlock() instanceof FluidBlock)) {
            return TypedActionResult.pass(stack);
        }
        if (world.isClient) return TypedActionResult.success(stack);
        if (!world.canPlayerModifyAt(player, pos) || !player.canPlaceOn(pos, hit.getSide(), stack)) {
            return TypedActionResult.fail(stack);
        }
        Entity spawned = type.spawnFromItemStack((ServerWorld) world, stack, player, pos,
                SpawnReason.SPAWN_EGG, false, false);
        if (spawned == null) return TypedActionResult.pass(stack);
        consume(player, stack);
        world.emitGameEvent(player, GameEvent.ENTITY_PLACE, spawned.getPos());
        return TypedActionResult.consume(stack);
    }

    private void consume(@Nullable PlayerEntity player, ItemStack stack) {
        if (player == null) return;
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!player.getAbilities().creativeMode) stack.decrement(1);
    }
}