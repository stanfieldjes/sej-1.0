package dev.sej.mixin;

import dev.sej.FenceIsolation;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FenceBlock.class)
public abstract class FenceBlockMixin extends HorizontalConnectingBlock {

    private FenceBlockMixin(float r1, float r2, float h1, float h2, float collisionHeight, AbstractBlock.Settings settings) {
        super(r1, r2, h1, h2, collisionHeight, settings);
    }

    @Inject(method = "appendProperties", at = @At("TAIL"))
    private void bettabeta$addIsolated(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(FenceIsolation.ISOLATED);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bettabeta$notIsolatedByDefault(AbstractBlock.Settings settings, CallbackInfo ci) {
        this.setDefaultState(this.getDefaultState().with(FenceIsolation.ISOLATED, false));
    }

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void bettabeta$sneakPlaceIsolated(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && ctx.shouldCancelInteraction()) {
            cir.setReturnValue(bettabeta$disconnect(state).with(FenceIsolation.ISOLATED, true));
        }
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("RETURN"), cancellable = true)
    private void bettabeta$stayIsolated(BlockState state, Direction direction, BlockState neighborState, WorldAccess world,
                                        BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (bettabeta$isIsolated(result)) {
            cir.setReturnValue(bettabeta$disconnect(result));
        }
    }

    @Inject(method = "canConnect", at = @At("HEAD"), cancellable = true)
    private void bettabeta$noConnectToIsolated(BlockState neighbor, boolean neighborIsFullSquare, Direction dir,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (bettabeta$isIsolated(neighbor)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean bettabeta$isIsolated(BlockState state) {
        return state != null && state.contains(FenceIsolation.ISOLATED) && state.get(FenceIsolation.ISOLATED);
    }

    @Unique
    private static BlockState bettabeta$disconnect(BlockState state) {
        return state.with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false);
    }
}