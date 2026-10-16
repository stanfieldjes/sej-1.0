package dev.sej;

import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public final class BowPredicateInit {
    public static void register() {
        ModelPredicateProviderRegistry.register(Items.BOW, new Identifier("pull"), (stack, world, entity, seed) -> {
            if (entity == null || entity.getActiveItem() != stack) return 0.0f;
            int ticks = stack.getMaxUseTime() - entity.getItemUseTimeLeft();
            if (ticks <= ArrowDrawInfo.STAGE_1_TICKS) {
                return ((float) ticks / ArrowDrawInfo.STAGE_1_TICKS) * 0.65f;
            }
            else if (ticks <= ArrowDrawInfo.STAGE_1_TICKS + ArrowDrawInfo.STAGE_DELAY_TICKS) {
                return 0.65f;
            }
            else {
                float s2Ticks = Math.min(ticks - ArrowDrawInfo.STAGE_1_TICKS - ArrowDrawInfo.STAGE_DELAY_TICKS, ArrowDrawInfo.STAGE_2_TICKS);
                float progress = s2Ticks / ArrowDrawInfo.STAGE_2_TICKS;
                return 0.65f + (progress * 0.25f);
            }
        });
    }
}