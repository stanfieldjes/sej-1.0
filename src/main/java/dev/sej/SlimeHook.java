package dev.sej;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Fishing rod + slime ball. With a slime ball in your inventory, the bobber sticks to blocks and
 * reeling in pulls you to it, or a hooked mob gets yanked toward you. One slime ball is used
 * each time that happens. Fishing in water is unchanged and uses nothing.
 */
public final class SlimeHook {
    /** How much of the distance becomes sideways speed when you grapple. Higher = harder pull. */
    public static final double GRAPPLE_PULL = 0.1;
    /** How far toward you a pulled mob lands: 0.5 = halfway. */
    public static final double MOB_PULL = 0.5;

    /** Small hop added to every pull so you clear ledges and mobs leave the ground. */
    private static final double LIFT = 0.3;
    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.91;
    private static final double FALL_DRAG = 0.98;
    private static final double MAX_UP = 1.8;
    private static final double MAX_SIDEWAYS = 2.5;

    private SlimeHook() {}

    public static boolean hasSlime(PlayerEntity player) {
        return player.getInventory().count(Items.SLIME_BALL) > 0;
    }

    /** Takes one slime ball (none in creative). */
    public static void useSlimeBall(PlayerEntity player) {
        if (player.getAbilities().creativeMode) return;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(Items.SLIME_BALL)) {
                stack.decrement(1);
                return;
            }
        }
    }

    /** Throws an entity toward a point, with enough upward speed to reach it if it's higher. */
    public static void launch(Entity entity, Vec3d target, double pull) {
        Vec3d d = target.subtract(entity.getPos());
        double up = d.y > 0 ? Math.sqrt(2 * GRAVITY * d.y) + LIFT : LIFT;
        setLaunch(entity, d.x * pull, up, d.z * pull);
    }

    /** Throws a hooked mob in an arc so it lands MOB_PULL of the way to the player. */
    public static void pullMob(Entity mob, PlayerEntity player) {
        Vec3d d = player.getPos().subtract(mob.getPos()).multiply(MOB_PULL);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);

        // Time in the air (ticks): longer throws get a higher arc.
        int t = MathHelper.clamp((int) Math.round(6 + flat * 0.5), 6, 20);

        // Starting speeds that land on the target after t ticks, with Minecraft's air drag.
        double sideways = (1 - AIR_DRAG) / (1 - Math.pow(AIR_DRAG, t));
        double c = GRAVITY * FALL_DRAG / (1 - FALL_DRAG);
        double up = (d.y + c * t) * (1 - FALL_DRAG) / (1 - Math.pow(FALL_DRAG, t)) - c;

        setLaunch(mob, d.x * sideways, Math.max(up, LIFT), d.z * sideways);
    }

    private static void setLaunch(Entity entity, double x, double up, double z) {
        double sideways = Math.sqrt(x * x + z * z);
        if (sideways > MAX_SIDEWAYS) {
            x *= MAX_SIDEWAYS / sideways;
            z *= MAX_SIDEWAYS / sideways;
        }
        entity.setVelocity(x, Math.min(up, MAX_UP), z);
        entity.velocityModified = true; // send it to the client (needed for players)
        entity.fallDistance = 0;
    }
}