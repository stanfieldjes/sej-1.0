package dev.sej;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TntBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.event.GameEvent;

/**
 * Gunpowder placed on the ground like redstone. Light it (flint and steel, fire charge, or
 * fire/lava next to it) and it burns along the trail, using itself up, and primes any TNT it touches.
 */
@SuppressWarnings("deprecation") // Mojang's "deprecated" here means "call via BlockState", not "don't override"
public class GunpowderTrailBlock extends Block {
    /** Ticks each piece burns before passing the flame on (20 = 1 second). */
    public static final int BURN_TICKS = 4;

    public static final EnumProperty<WireConnection> NORTH = Properties.NORTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> EAST = Properties.EAST_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> SOUTH = Properties.SOUTH_WIRE_CONNECTION;
    public static final EnumProperty<WireConnection> WEST = Properties.WEST_WIRE_CONNECTION;
    public static final BooleanProperty LIT = Properties.LIT;

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);

    public static final GunpowderTrailBlock BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(Sej.MOD_ID, "gunpowder_trail"),
            new GunpowderTrailBlock(AbstractBlock.Settings.create()
                    .noCollision()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.SAND)
                    .luminance(state -> state.get(LIT) ? 10 : 0)
                    .pistonBehavior(PistonBehavior.DESTROY)));

    /** Loads the class so the block registers. Call from onInitialize. */
    public static void register() {}

    public GunpowderTrailBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(NORTH, WireConnection.NONE).with(EAST, WireConnection.NONE)
                .with(SOUTH, WireConnection.NONE).with(WEST, WireConnection.NONE)
                .with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, LIT);
    }

    // ---------------------------------------------------------------- placing

    /** Right-click with gunpowder (called from GunpowderPlaceMixin). */
    public static ActionResult place(ItemUsageContext context) {
        ItemPlacementContext ctx = new ItemPlacementContext(context);
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        if (!ctx.canPlace()) return ActionResult.FAIL;
        BlockState state = BLOCK.getPlacementState(ctx);
        if (state == null || !state.canPlaceAt(world, pos)) return ActionResult.FAIL;

        if (!world.isClient) {
            PlayerEntity player = ctx.getPlayer();
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
            BlockSoundGroup sounds = state.getSoundGroup();
            world.playSound(null, pos, sounds.getPlaceSound(), SoundCategory.BLOCKS,
                    (sounds.getVolume() + 1.0F) / 2.0F, sounds.getPitch() * 0.8F);
            world.emitGameEvent(player, GameEvent.BLOCK_PLACE, pos);
            if (player == null || !player.getAbilities().creativeMode) {
                ctx.getStack().decrement(1);
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.withConnections(ctx.getWorld(), ctx.getBlockPos(), this.getDefaultState());
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockState belowState = world.getBlockState(below);
        return belowState.isSideSolidFullSquare(world, below, Direction.UP) || belowState.isOf(Blocks.HOPPER);
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(Items.GUNPOWDER);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // ---------------------------------------------------------------- connections (same shapes as redstone)

    private static boolean isTrail(BlockState state) {
        return state.isOf(BLOCK);
    }

    private WireConnection connection(BlockView world, BlockPos pos, Direction dir) {
        BlockPos side = pos.offset(dir);
        BlockState sideState = world.getBlockState(side);
        boolean sideSolid = sideState.isSolidBlock(world, side);
        boolean aboveSolid = world.getBlockState(pos.up()).isSolidBlock(world, pos.up());

        if (sideSolid && !aboveSolid && isTrail(world.getBlockState(side.up()))) return WireConnection.UP;
        if (isTrail(sideState) || sideState.isOf(Blocks.TNT)) return WireConnection.SIDE;
        if (!sideSolid && isTrail(world.getBlockState(side.down()))) return WireConnection.SIDE;
        return WireConnection.NONE;
    }

    private BlockState withConnections(BlockView world, BlockPos pos, BlockState state) {
        WireConnection n = connection(world, pos, Direction.NORTH);
        WireConnection e = connection(world, pos, Direction.EAST);
        WireConnection s = connection(world, pos, Direction.SOUTH);
        WireConnection w = connection(world, pos, Direction.WEST);

        // A single connection draws as a straight line through the block, like redstone.
        boolean ns = n != WireConnection.NONE || s != WireConnection.NONE;
        boolean ew = e != WireConnection.NONE || w != WireConnection.NONE;
        int count = (n != WireConnection.NONE ? 1 : 0) + (e != WireConnection.NONE ? 1 : 0)
                + (s != WireConnection.NONE ? 1 : 0) + (w != WireConnection.NONE ? 1 : 0);
        if (count == 1) {
            if (ns && n == WireConnection.NONE) n = WireConnection.SIDE;
            if (ns && s == WireConnection.NONE) s = WireConnection.SIDE;
            if (ew && e == WireConnection.NONE) e = WireConnection.SIDE;
            if (ew && w == WireConnection.NONE) w = WireConnection.SIDE;
        }
        return state.with(NORTH, n).with(EAST, e).with(SOUTH, s).with(WEST, w);
    }

    /** Trails one block up/down diagonally don't get a neighbor update, so refresh them by hand. */
    private void refreshDiagonals(World world, BlockPos pos) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy += 2) {
                BlockPos p = pos.offset(dir).up(dy);
                BlockState s = world.getBlockState(p);
                if (isTrail(s)) {
                    BlockState updated = this.withConnections(world, p, s);
                    if (updated != s) world.setBlockState(p, updated, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return this.withConnections(world, pos, state);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (oldState.isOf(this) || world.isClient) return;
        this.refreshDiagonals(world, pos);
        if (touchesFire(world, pos)) light((ServerWorld) world, pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.isOf(this) && !world.isClient) {
            this.refreshDiagonals(world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ---------------------------------------------------------------- burning

    private static boolean touchesFire(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState s = world.getBlockState(pos.offset(dir));
            if (s.isIn(BlockTags.FIRE) || s.getFluidState().isIn(FluidTags.LAVA)) return true;
        }
        return false;
    }

    /** Lights the trail at pos (used by dispensers). Returns false if there's no unlit trail there. */
    public static boolean ignite(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!isTrail(state) || state.get(LIT)) return false;
        light(world, pos, state);
        return true;
    }

    private static void light(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.get(LIT)) return;
        world.setBlockState(pos, state.with(LIT, true), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, BLOCK, BURN_TICKS);
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS,
                0.25F, 1.8F + world.random.nextFloat() * 0.2F);
        world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                3, 0.2, 0.0, 0.2, 0.01);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient && !state.get(LIT) && touchesFire(world, pos)) {
            light((ServerWorld) world, pos, state);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        boolean flint = stack.isOf(Items.FLINT_AND_STEEL);
        if (!flint && !stack.isOf(Items.FIRE_CHARGE)) {
            return super.onUse(state, world, pos, player, hand, hit);
        }
        if (state.get(LIT)) return ActionResult.PASS;

        if (!world.isClient) {
            light((ServerWorld) world, pos, state);
            world.playSound(null, pos,
                    flint ? SoundEvents.ITEM_FLINTANDSTEEL_USE : SoundEvents.ITEM_FIRECHARGE_USE,
                    SoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.4F + 0.8F);
            player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            if (!player.getAbilities().creativeMode) {
                if (flint) {
                    stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
                } else {
                    stack.decrement(1);
                }
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(LIT)) return;

        // Pass the flame to the next pieces of the trail (same level, or one up/down a step).
        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos p = pos.offset(dir).up(dy);
                BlockState s = world.getBlockState(p);
                if (isTrail(s)) light(world, p, s);
            }
        }

        // Prime any TNT touching this piece.
        for (Direction dir : Direction.values()) {
            BlockPos p = pos.offset(dir);
            if (world.getBlockState(p).isOf(Blocks.TNT)) {
                TntBlock.primeTnt(world, p);
                world.removeBlock(p, false);
            }
        }

        // This piece is used up.
        world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                4, 0.2, 0.05, 0.2, 0.01);
        world.removeBlock(pos, false);
    }
}