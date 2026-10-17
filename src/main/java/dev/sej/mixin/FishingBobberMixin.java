package dev.sej.mixin;

import dev.sej.SlimeHook;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slime ball grappling and mob pulling for the normal fishing rod. See SlimeHook. */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberMixin extends ProjectileEntity {

    /** True while the bobber is stuck to a block. Synced so the client holds it still too. */
    @Unique
    private static final TrackedData<Boolean> SEJ_STUCK =
            DataTracker.registerData(FishingBobberEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    @Shadow public abstract @Nullable PlayerEntity getPlayerOwner();

    @Shadow public abstract @Nullable Entity getHookedEntity();

    protected FishingBobberMixin(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void sej$trackStuck(CallbackInfo ci) {
        this.dataTracker.startTracking(SEJ_STUCK, false);
    }

    /** Stick to the first block hit (not water) if the owner has a slime ball. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void sej$stick(CallbackInfo ci) {
        if (this.isRemoved()) return;

        if (this.dataTracker.get(SEJ_STUCK)) {
            if (this.getHookedEntity() != null) {
                if (!this.getWorld().isClient) this.dataTracker.set(SEJ_STUCK, false);
            } else {
                // Undo this tick's movement so it stays put (prevX/Y/Z = where it started the tick).
                this.setPosition(this.prevX, this.prevY, this.prevZ);
                this.setVelocity(Vec3d.ZERO);
            }
            return;
        }

        if (this.getWorld().isClient || this.getHookedEntity() != null) return;
        if (!this.horizontalCollision && !this.verticalCollision) return;
        if (this.getWorld().getFluidState(this.getBlockPos()).isIn(FluidTags.WATER)) return;

        PlayerEntity owner = this.getPlayerOwner();
        if (owner == null || !SlimeHook.hasSlime(owner)) return;

        this.dataTracker.set(SEJ_STUCK, true);
        this.setVelocity(Vec3d.ZERO);
    }

    /** Reeling in: pull yourself to a stuck bobber, or pull a hooked mob to you. */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sej$slimePull(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        if (this.getWorld().isClient) return;
        PlayerEntity owner = this.getPlayerOwner();
        if (owner == null || !SlimeHook.hasSlime(owner)) return;
        if (owner.squaredDistanceTo(this) > 32 * 32) return; // vanilla's line limit

        Entity hooked = this.getHookedEntity();
        if (hooked instanceof LivingEntity) {
            SlimeHook.pullMob(hooked, owner);
        } else if (hooked == null && this.dataTracker.get(SEJ_STUCK)) {
            SlimeHook.launch(owner, this.getPos(), SlimeHook.GRAPPLE_PULL);
        } else {
            return; // water, dropped items, or nothing hit: normal fishing
        }

        SlimeHook.useSlimeBall(owner);
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_SLIME_SQUISH_SMALL, SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.discard();
        cir.setReturnValue(2); // rod durability used, same as reeling in from the ground
    }
}