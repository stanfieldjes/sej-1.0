package dev.sej.mixin;

import dev.sej.Sej;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements Sej.HighScoreHolder {

    @Unique private int sej$highScore;

    @Override
    public int sej$getHighScore() {
        return this.sej$highScore;
    }

    @Override
    public void sej$setHighScore(int highScore) {
        this.sej$highScore = Math.max(0, highScore);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void sej$writeHighScore(NbtCompound nbt, CallbackInfo ci) {
        nbt.putInt("sejHighScore", this.sej$highScore);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void sej$readHighScore(NbtCompound nbt, CallbackInfo ci) {
        this.sej$highScore = nbt.getInt("sejHighScore");
    }
}