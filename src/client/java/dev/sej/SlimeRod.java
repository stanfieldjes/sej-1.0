package dev.sej;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The fishing rod shows a slime glob on its hook while you have a slime ball, so you know
 * it will grapple. The rod model is replaced while models load, so this works with any
 * resource pack; the rod texture itself still comes from the pack.
 */
public final class SlimeRod {
    private static final Identifier SLIMED = new Identifier("sej", "slimed");
    private static final Identifier SLIMED_MODEL = new Identifier("sej", "item/fishing_rod_slimed");

    /** How far the glob sits out from the rod, so the two never flicker over each other in hand. */
    private static final float PUSH = 0.001F;

    // "cast" is listed last so a cast rod always shows the normal cast texture.
    private static final String ROD_JSON = "{\"parent\":\"minecraft:item/handheld_rod\","
            + "\"textures\":{\"layer0\":\"minecraft:item/fishing_rod\"},"
            + "\"overrides\":["
            + "{\"predicate\":{\"sej:slimed\":1},\"model\":\"sej:item/fishing_rod_slimed\"},"
            + "{\"predicate\":{\"cast\":1},\"model\":\"minecraft:item/fishing_rod_cast\"}"
            + "]}";

    private SlimeRod() {}

    /** Call from the client entrypoint. */
    public static void register() {
        ModelPredicateProviderRegistry.register(Items.FISHING_ROD, SLIMED,
                (stack, world, entity, seed) -> entity instanceof PlayerEntity player && SlimeHook.hasSlime(player) ? 1.0F : 0.0F);

        ModelLoadingPlugin.register(plugin -> {
            plugin.addModels(SLIMED_MODEL);

            plugin.modifyModelOnLoad().register((model, context) -> {
                if (context.id() instanceof ModelIdentifier id && "inventory".equals(id.getVariant())
                        && "minecraft".equals(id.getNamespace()) && "fishing_rod".equals(id.getPath())) {
                    JsonUnbakedModel rod = JsonUnbakedModel.deserialize(ROD_JSON);
                    rod.id = id.toString();
                    return rod;
                }
                return model;
            });

            plugin.modifyModelAfterBake().register((model, context) ->
                    model != null && SLIMED_MODEL.equals(context.id()) ? new Slimed(model) : model);
        });
    }

    /**
     * The slimed rod with the glob (texture layer 1) nudged outward on every side. Both layers
     * are flat at the same depth, which looks fine in the inventory but flickers in 3D.
     */
    private static final class Slimed implements BakedModel {
        private final BakedModel wrapped;
        private final List<BakedQuad> quads = new ArrayList<>();

        Slimed(BakedModel wrapped) {
            this.wrapped = wrapped;
            List<BakedQuad> all = wrapped.getQuads(null, null, Random.create());

            // Size of the glob, from its edge faces (the front and back faces cover the whole 16x16).
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (BakedQuad quad : all) {
                if (quad.getColorIndex() != 1 || quad.getFace().getAxis() == Direction.Axis.Z) continue;
                int[] data = quad.getVertexData();
                int stride = data.length / 4;
                for (int v = 0; v < 4; v++) {
                    float x = Float.intBitsToFloat(data[v * stride]);
                    float y = Float.intBitsToFloat(data[v * stride + 1]);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }

            boolean found = minX <= maxX;
            float cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
            // Grow the glob by PUSH on every side as one solid piece, so its faces stay joined.
            float sx = found ? 1 + PUSH / Math.max((maxX - minX) / 2, 0.01F) : 1;
            float sy = found ? 1 + PUSH / Math.max((maxY - minY) / 2, 0.01F) : 1;
            float sz = 1 + PUSH / (1 / 32F); // generated items are 1/16 thick

            for (BakedQuad quad : all) {
                this.quads.add(quad.getColorIndex() == 1 && found ? grow(quad, cx, cy, sx, sy, sz) : quad);
            }
        }

        private static BakedQuad grow(BakedQuad quad, float cx, float cy, float sx, float sy, float sz) {
            int[] data = quad.getVertexData().clone();
            int stride = data.length / 4;
            for (int v = 0; v < 4; v++) {
                int i = v * stride;
                data[i] = Float.floatToRawIntBits(cx + (Float.intBitsToFloat(data[i]) - cx) * sx);
                data[i + 1] = Float.floatToRawIntBits(cy + (Float.intBitsToFloat(data[i + 1]) - cy) * sy);
                data[i + 2] = Float.floatToRawIntBits(0.5F + (Float.intBitsToFloat(data[i + 2]) - 0.5F) * sz);
            }
            return new BakedQuad(data, quad.getColorIndex(), quad.getFace(), quad.getSprite(), quad.hasShade());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
            return face == null ? this.quads : this.wrapped.getQuads(state, face, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.wrapped.useAmbientOcclusion();
        }

        @Override
        public boolean hasDepth() {
            return this.wrapped.hasDepth();
        }

        @Override
        public boolean isSideLit() {
            return this.wrapped.isSideLit();
        }

        @Override
        public boolean isBuiltin() {
            return this.wrapped.isBuiltin();
        }

        @Override
        public Sprite getParticleSprite() {
            return this.wrapped.getParticleSprite();
        }

        @Override
        public ModelTransformation getTransformation() {
            return this.wrapped.getTransformation();
        }

        @Override
        public ModelOverrideList getOverrides() {
            return this.wrapped.getOverrides();
        }
    }
}