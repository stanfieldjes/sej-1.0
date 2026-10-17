package dev.sej;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/** Server-side rules that used to live in KubeJS scripts. */
public final class PackRules {
    /** Chance that hoeing grass drops wheat seeds. */
    public static final float SEED_CHANCE = 0.5F;

    private PackRules() {}

    /** Call from onInitialize. */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(PackRules::applyGameRules);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> disableOffhand(server));
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            seedsFromTilling(player, world, hand, hit);
            return ActionResult.PASS;
        });
    }

    /** Set every time a world loads, like the old script did. */
    private static void applyGameRules(MinecraftServer server) {
        GameRules rules = server.getGameRules();
        rules.get(GameRules.DO_INSOMNIA).set(false, server);
        rules.get(GameRules.DO_PATROL_SPAWNING).set(false, server);
        rules.get(GameRules.DO_TRADER_SPAWNING).set(false, server);
        rules.get(GameRules.SPAWN_RADIUS).set(0, server);
        rules.get(GameRules.REDUCED_DEBUG_INFO).set(true, server);
        rules.get(GameRules.BLOCK_EXPLOSION_DROP_DECAY).set(false, server);
        rules.get(GameRules.MOB_EXPLOSION_DROP_DECAY).set(false, server);
    }

    /** Runs "/offhand disable @a" when a player joins, if a mod provides that command. */
    private static void disableOffhand(MinecraftServer server) {
        CommandManager commands = server.getCommandManager();
        if (commands.getDispatcher().getRoot().getChild("offhand") == null) return;
        commands.executeWithPrefix(server.getCommandSource().withSilent(), "offhand disable @a");
    }

    /** Right-clicking grass with a hoe sometimes turns up wheat seeds. */
    private static void seedsFromTilling(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient) return;
        if (!(player.getStackInHand(hand).getItem() instanceof HoeItem)) return;
        BlockPos pos = hit.getBlockPos();
        if (!world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK)) return;

        if (world.random.nextFloat() < SEED_CHANCE) {
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                    new ItemStack(Items.WHEAT_SEEDS)));
        }
    }
}