package dev.sej;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;


public final class GroundSizeFix {
    /** Size of a normal dropped block (vanilla's block model). */
    private static final float BLOCK_SCALE = 0.25F;
    /** Shrunk copies of models, so they're only built once per resource reload. */
    private static final Map<BakedModel, BakedModel> CACHE = new WeakHashMap<>();

    private GroundSizeFix() {}

    /** Called by ItemEntityGroundSizeMixin for every dropped item it renders. */
    public static BakedModel adjust(ItemStack stack, BakedModel model) {
        if (!(stack.getItem() instanceof BlockItem) || !model.hasDepth()) return model;
        Vector3f scale = model.getTransformation().ground.scale;
        float smallest = Math.min(scale.x(), Math.min(scale.y(), scale.z()));
        if (smallest <= BLOCK_SCALE + 0.01F) return model;
        return CACHE.computeIfAbsent(model, Shrunk::new);
    }

    /** The same model, with its ground transform scaled down to normal block size. */
    private static final class Shrunk extends ForwardingBakedModel {
        private final ModelTransformation transformation;

        Shrunk(BakedModel original) {
            this.wrapped = original;
            ModelTransformation t = original.getTransformation();
            Transformation g = t.ground;
            float factor = BLOCK_SCALE / Math.max(g.scale.x(), Math.max(g.scale.y(), g.scale.z()));
            Transformation ground = new Transformation(
                    new Vector3f(g.rotation),
                    new Vector3f(g.translation).mul(factor),
                    new Vector3f(g.scale).mul(factor));
            this.transformation = new ModelTransformation(
                    t.thirdPersonLeftHand, t.thirdPersonRightHand,
                    t.firstPersonLeftHand, t.firstPersonRightHand,
                    t.head, t.gui, ground, t.fixed);
        }

        @Override
        public ModelTransformation getTransformation() {
            return this.transformation;
        }
    }
}