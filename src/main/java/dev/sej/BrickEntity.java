package dev.sej;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/** A thrown brick: like a snowball, but drops faster, flies slower and actually hurts. */
public class BrickEntity extends ThrownItemEntity {
    /** Snowballs and eggs use 0.03. Higher = drops faster. */
    public static final float GRAVITY = 0.05F;
    /** Damage on hit, in half-hearts. Snowballs/eggs do 0. */
    public static final float DAMAGE = 2.0F;

    /** Blocks a brick smashes through. Edit data/sej/tags/blocks/brick_breakable.json to change the list. */
    public static final TagKey<Block> BREAKABLE =
            TagKey.of(RegistryKeys.BLOCK, new Identifier(Sej.MOD_ID, "brick_breakable"));

    public BrickEntity(EntityType<? extends BrickEntity> type, World world) {
        super(type, world);
    }

    public BrickEntity(World world, LivingEntity owner) {
        super(ThrowableBricks.BRICK_ENTITY, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.BRICK;
    }

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 3) { // hit particles: brick crumbs bursting outward
            ItemStackParticleEffect particle = new ItemStackParticleEffect(ParticleTypes.ITEM, this.getStack());
            for (int i = 0; i < 8; i++) {
                double vx = (this.random.nextDouble() - 0.5) * 0.2;
                double vy = this.random.nextDouble() * 0.15 + 0.05;
                double vz = (this.random.nextDouble() - 0.5) * 0.2;
                this.getWorld().addParticle(particle,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.2,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.2,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.2,
                        vx, vy, vz);
            }
        } else {
            super.handleStatus(status);
        }
    }

    /** Players need build rights here (not adventure mode, not spawn protection); otherwise follow mobGriefing. */
    private boolean canBreak(BlockPos pos) {
        if (this.getOwner() instanceof PlayerEntity player) {
            return player.canModifyBlocks() && this.getWorld().canPlayerModifyAt(player, pos);
        }
        return this.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        Entity target = hit.getEntity();
        target.damage(this.getDamageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onCollision(HitResult hit) {
        // Fragile blocks (glass, glowstone, ...): smash them and keep flying, slowed down.
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (this.getWorld().getBlockState(pos).isIn(BREAKABLE)) {
                if (!this.getWorld().isClient && this.canBreak(pos)) {
                    this.getWorld().breakBlock(pos, true, this.getOwner());
                    this.setVelocity(this.getVelocity().multiply(0.6));
                    return;
                }
                if (this.getWorld().isClient) return; // server decides; position syncs back
            }
        }

        super.onCollision(hit);
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte) 3);
            this.playSound(SoundEvents.BLOCK_STONE_HIT, 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
            this.discard();
        }
    }
}