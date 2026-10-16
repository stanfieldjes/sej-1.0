package dev.sej.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sej.ArmorCalculator;
import dev.sej.DamageCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Unique private static final Identifier ICONS = new Identifier("textures/gui/icons.png");
    @Unique private static final int ARMOR_FULL_U = 34;
    @Unique private static final int ARMOR_FULL_V = 9;
    @Unique private static final int ICON_SIZE = 9;

    @Unique private static final ItemStack DURABILITY_REFERENCE = new ItemStack(Items.IRON_CHESTPLATE);

    @Unique private static final int COLOR_TRACK = 0xFF000000;

    @Unique private static final int[] SLOT_Y = {8, 26, 44, 62};
    @Unique private static final int ARMOR_SLOT_X = 8;

    @Unique private static final DamageCategory[] CATEGORIES = {
            DamageCategory.BLAST, DamageCategory.COMBAT, DamageCategory.FIRE, DamageCategory.FALL
    };

    @Unique private static final String[] CATEGORY_NAMES = {
            "Blast", "Combat", "Fire", "Fall"
    };

    @Unique private static final int MAX_ARMOR_BARS = 3;

    @Unique private static final int BAR_W = 2;
    @Unique private static final int BAR_H = 13;
    @Unique private static final int SLOT_BAR_GAP = 2;

    @Unique private static final int TOOLTIP_LEFT_OFFSET = 20;
    @Unique private static final int TOOLTIP_ICON_GAP = 4;

    @Inject(method = "render", at = @At("TAIL"))
    private void renderArmorGauges(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        HandledScreenAccessor accessor = (HandledScreenAccessor) this;
        int guiX = accessor.getX();
        int guiY = accessor.getY();

        int hoveredIndex = -1;

        for (int i = 0; i < CATEGORIES.length; i++) {
            DamageCategory category = CATEGORIES[i];
            int slotY = guiY + SLOT_Y[i];

            int barX = guiX + ARMOR_SLOT_X - SLOT_BAR_GAP - BAR_W;
            int barY = slotY + (16 - BAR_H) / 2;

            float multiplier = ArmorCalculator.computeDamageMultiplier(player, category);
            float fillFraction = (1.0f - multiplier) / ArmorCalculator.MAX_STRENGTH_VALUE;
            fillFraction = Math.max(0.0f, Math.min(1.0f, fillFraction));

            if (mouseX >= barX && mouseX < barX + BAR_W
                    && mouseY >= barY && mouseY < barY + BAR_H) {
                hoveredIndex = i;
            }

            context.fill(barX, barY, barX + BAR_W, barY + BAR_H, COLOR_TRACK);

            int filledHeight = Math.round(BAR_H * fillFraction);
            if (filledHeight > 0) {
                int fillColor = 0xFF000000 | sej$durabilityBarColor(fillFraction);
                int fillY = barY + (BAR_H - filledHeight);
                context.fill(barX + 1, fillY, barX + BAR_W, barY + BAR_H, fillColor);
            }
        }

        if (hoveredIndex != -1) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            String categoryText = CATEGORY_NAMES[hoveredIndex];

            float multiplier = ArmorCalculator.computeDamageMultiplier(player, CATEGORIES[hoveredIndex]);
            float fillFraction = (1.0f - multiplier) / ArmorCalculator.MAX_STRENGTH_VALUE;
            fillFraction = Math.max(0.0f, Math.min(1.0f, fillFraction));
            String valueText = String.format("%.2f", fillFraction * MAX_ARMOR_BARS);

            TooltipComponent categoryComponent = new TextLineComponent(categoryText);
            TooltipComponent valueComponent = new ValueIconComponent(valueText);

            int categoryWidth = textRenderer.getWidth(categoryText);
            int valueLineWidth = valueComponent.getWidth(textRenderer);
            int tooltipTextWidth = Math.max(categoryWidth, valueLineWidth);

            int tooltipX = mouseX - tooltipTextWidth - TOOLTIP_LEFT_OFFSET;

            ((DrawContextInvoker) context).sej$drawTooltip(
                    textRenderer,
                    List.of(categoryComponent, valueComponent),
                    tooltipX, mouseY,
                    HoveredTooltipPositioner.INSTANCE
            );
        }
    }

    @Unique
    private int sej$durabilityBarColor(float fraction) {
        int maxDamage = DURABILITY_REFERENCE.getMaxDamage();
        int damage = Math.round((1.0f - fraction) * maxDamage);
        damage = Math.max(0, Math.min(maxDamage, damage));
        DURABILITY_REFERENCE.setDamage(damage);
        return DURABILITY_REFERENCE.getItemBarColor();
    }

    private record TextLineComponent(String text) implements TooltipComponent {

        @Override
            public int getHeight() {
                return ICON_SIZE;
            }

            @Override
            public int getWidth(TextRenderer textRenderer) {
                return textRenderer.getWidth(text);
            }

            @Override
            public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
                context.drawText(textRenderer, text, x, y, 0xFFFFFFFF, false);
            }
        }

    private record ValueIconComponent(String text) implements TooltipComponent {

        @Override
            public int getHeight() {
                return ICON_SIZE;
            }

            @Override
            public int getWidth(TextRenderer textRenderer) {
                return textRenderer.getWidth(text) + TOOLTIP_ICON_GAP + ICON_SIZE;
            }

            @Override
            public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

                context.drawTexture(ICONS, x, y - 1, ARMOR_FULL_U, ARMOR_FULL_V, ICON_SIZE, ICON_SIZE);

                int textX = x + ICON_SIZE + TOOLTIP_ICON_GAP;
                context.drawText(textRenderer, text, textX, y, 0xFFFFFFFF, false);
            }
        }
}