package dev.sej.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudInfoMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render", at = @At("TAIL"))
    private void hudinfo$renderHeldItemInfo(DrawContext context, float tickDelta, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) return;

        if (this.client.options.hudHidden) return;

        ItemStack held = player.getMainHandStack();
        String line;

        if (held.isOf(Items.COMPASS)) {
            BlockPos pos = player.getBlockPos();
            BlockPos spawn = player.getWorld().getSpawnPos();
            line = String.format("%d, %d, %d", pos.getX() - spawn.getX(), pos.getY(), pos.getZ() - spawn.getZ());
        } else if (held.isOf(Items.CLOCK)) {
            long worldTime = player.getWorld().getTimeOfDay();
            long day = worldTime / 24000L;
            long timeOfDay = worldTime % 24000L;

            int hours = (int) ((timeOfDay / 1000L + 6L) % 24L);
            int minutes = (int) ((timeOfDay % 1000L) * 60L / 1000L);

            line = String.format("Day %d  %02d:%02d", day, hours, minutes);
        } else {
            return;
        }

        TextRenderer textRenderer = this.client.textRenderer;
        int x = context.getScaledWindowWidth() / 2;
        int y = context.getScaledWindowHeight() - 36 - textRenderer.fontHeight;

        context.drawCenteredTextWithShadow(textRenderer, line, x, y, 0xFFFFFFFF);
    }
}
