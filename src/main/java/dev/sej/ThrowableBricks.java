package dev.sej;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

/** Right-click with a brick to throw it. */
public final class ThrowableBricks {
    /** Snowballs and eggs are thrown at 1.5. */
    public static final float THROW_SPEED = 1.1F;
    /** Ticks between throws (20 = 1 second). Snowballs have none. */
    public static final int COOLDOWN_TICKS = 10;

    public static final EntityType<BrickEntity> BRICK_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(Sej.MOD_ID, "brick"),
            EntityType.Builder.<BrickEntity>create(BrickEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build("brick"));

    private ThrowableBricks() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.BRICK) || player.isSpectator()
                    || player.getItemCooldownManager().isCoolingDown(Items.BRICK)) {
                return TypedActionResult.pass(stack);
            }

            // Same throw sound as snowballs/eggs. 0.5 is the lowest pitch Minecraft allows
            // (anything lower is clamped), so this is as deep as the vanilla file can go.
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.5F);

            if (!world.isClient) {
                BrickEntity brick = new BrickEntity(world, player);
                brick.setItem(stack);
                brick.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, THROW_SPEED, 1.0F);
                world.spawnEntity(brick);
            }

            player.getItemCooldownManager().set(Items.BRICK, COOLDOWN_TICKS);
            player.incrementStat(Stats.USED.getOrCreateStat(Items.BRICK));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return TypedActionResult.success(stack, world.isClient());
        });
    }
}