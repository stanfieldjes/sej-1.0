package dev.sej;

import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Redstone torches act as lightning rods. When one is struck:
 *  - the cobblestone directly under the torch turns into glowstone,
 *  - so does the cobblestone under every piece of powered redstone dust connected to it,
 *  - then the torch and that powered dust burn away (no drops).
 * Unpowered dust (out of range of the torch's signal) isn't followed.
 */
public final class LightningTorches {

    public static final Identifier POI_ID = new Identifier("bettabeta", "redstone_torch_rod");
    public static final RegistryKey<PointOfInterestType> POI_KEY =
            RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, POI_ID);

    /** Safety cap on how much dust one strike can follow. */
    private static final int MAX_WIRE = 512;

    private LightningTorches() {}

    /** Registers redstone torches as points of interest so lightning can find them, like vanilla lightning rods. */
    public static void register() {
        PointOfInterestHelper.register(POI_ID, 0, 1, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH);
    }

    public static boolean isTorch(BlockState state) {
        return state.isOf(Blocks.REDSTONE_TORCH) || state.isOf(Blocks.REDSTONE_WALL_TORCH);
    }

    private static boolean isPoweredWire(BlockState state) {
        return state.isOf(Blocks.REDSTONE_WIRE) && state.get(RedstoneWireBlock.POWER) > 0;
    }

    public static void onStrike(ServerWorld world, BlockPos torchPos) {
        if (!isTorch(world.getBlockState(torchPos))) return;

        // Work out everything before touching the world: breaking the torch would depower the dust.
        Set<BlockPos> wires = collectPoweredWire(world, torchPos);

        List<BlockPos> below = new ArrayList<>(wires.size() + 1);
        below.add(torchPos.down());
        for (BlockPos wire : wires) below.add(wire.down());

        for (BlockPos wire : wires) world.breakBlock(wire, false);
        world.breakBlock(torchPos, false);

        for (BlockPos pos : below) {
            if (world.getBlockState(pos).isOf(Blocks.COBBLESTONE)) {
                world.setBlockState(pos, Blocks.GLOWSTONE.getDefaultState());
            }
        }
    }

    /** Powered dust reachable from the torch, following the dust's own connections (including up and down slopes). */
    private static Set<BlockPos> collectPoweredWire(ServerWorld world, BlockPos torchPos) {
        Set<BlockPos> found = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (Direction dir : Direction.Type.HORIZONTAL) {
            visit(world, torchPos.offset(dir), found, queue);
        }

        while (!queue.isEmpty() && found.size() < MAX_WIRE) {
            BlockPos pos = queue.poll();
            BlockState state = world.getBlockState(pos);

            for (Direction dir : Direction.Type.HORIZONTAL) {
                WireConnection connection = state.get(RedstoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(dir));
                if (!connection.isConnected()) continue;

                BlockPos side = pos.offset(dir);
                visit(world, side, found, queue);
                visit(world, side.down(), found, queue);
                if (connection == WireConnection.UP) visit(world, side.up(), found, queue);
            }
        }
        return found;
    }

    private static void visit(ServerWorld world, BlockPos pos, Set<BlockPos> found, Deque<BlockPos> queue) {
        if (found.size() >= MAX_WIRE || found.contains(pos)) return;
        if (!isPoweredWire(world.getBlockState(pos))) return;
        BlockPos immutable = pos.toImmutable();
        found.add(immutable);
        queue.add(immutable);
    }
}