package dev.sej;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.item.SpawnEggItem;

public class SejClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BowPredicateInit.register();
		UniformDyes.register();
		EntityRendererRegistry.register(ThrowableBricks.BRICK_ENTITY, FlyingItemEntityRenderer::new);

		BlockRenderLayerMap.INSTANCE.putBlock(GunpowderTrailBlock.BLOCK, RenderLayer.getCutout());
		ColorProviderRegistry.BLOCK.register(
				(state, world, pos, tintIndex) -> state.get(GunpowderTrailBlock.LIT) ? 0xFF8A2A : 0x6B6B6B,
				GunpowderTrailBlock.BLOCK);

		MobSpawnEggItem.menuOpener = hand -> MinecraftClient.getInstance().setScreen(new MobSelectScreen(hand));
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			EntityType<?> mob = MobSpawnEggItem.getMob(stack);
			SpawnEggItem vanilla = mob == null ? null : SpawnEggItem.forEntity(mob);
			if (vanilla == null) return tintIndex == 0 ? 0xE8E0C8 : 0xA89870; // no mob picked yet
			return vanilla.getColor(tintIndex);
		}, MobSpawnEggItem.ITEM);

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new ItemList());

		ClientPlayNetworking.registerGlobalReceiver(Sej.SYNC_SCORE,
				(client, handler, buf, responseSender) -> {
					int score = buf.readVarInt();
					int highScore = buf.readVarInt();
					client.execute(() -> {
						if (client.player != null) {
							client.player.setScore(score);
							((Sej.HighScoreHolder) client.player)
									.sej$setHighScore(highScore);
						}
					});
				});
	}
}