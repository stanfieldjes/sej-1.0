package dev.sej;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sej implements ModInitializer {
	public static final String MOD_ID = "sej";
	public static final String VERSION = "1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier SYNC_SCORE = new Identifier("sej", "sync_score");
	public static final Identifier SET_CREATIVE_CURSOR = new Identifier("sej", "set_creative_cursor");

	public static void add(PlayerEntity player, int amount) {
		if (amount == 0) return;
		if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
		if (serverPlayer.interactionManager.getGameMode() != GameMode.SURVIVAL) return;

		serverPlayer.addScore(amount);

		HighScoreHolder holder = (HighScoreHolder) serverPlayer;
		if (serverPlayer.getScore() > holder.sej$getHighScore()) {
			holder.sej$setHighScore(serverPlayer.getScore());
		}

		syncScore(serverPlayer);
	}

	public static void syncScore(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(player.getScore());
		buf.writeVarInt(((HighScoreHolder) player).sej$getHighScore());
		ServerPlayNetworking.send(player, SYNC_SCORE, buf);
	}

	private static int killValue(LivingEntity entity) {
		if (entity instanceof PlayerEntity) return 0;
		SpawnGroup group = entity.getType().getSpawnGroup();
		if (group == SpawnGroup.MONSTER) return 10;
		return group == SpawnGroup.MISC ? 0 : 5;
	}

	public interface HighScoreHolder {
		int sej$getHighScore();
		void sej$setHighScore(int highScore);
	}

	@Override
	public void onInitialize() {
		EntitySleepEvents.ALLOW_SLEEP_TIME.register(
				(player, sleepingPos, vanillaResult) -> ActionResult.SUCCESS
		);

		PlayerBlockBreakEvents.AFTER.register(
				(world, player, pos, state, be) -> add(player, 1));

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Entity attacker = source.getAttacker();
			if (attacker instanceof PlayerEntity killer && killer != entity) {
				add(killer, killValue(entity));
			}
		});

		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			((HighScoreHolder) newPlayer).sej$setHighScore(
					((HighScoreHolder) oldPlayer).sej$getHighScore());
			newPlayer.setScore(alive ? oldPlayer.getScore() : 0);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register(
				(oldPlayer, newPlayer, alive) -> syncScore(newPlayer));
		ServerPlayConnectionEvents.JOIN.register(
				(handler, sender, server) -> syncScore(handler.getPlayer()));

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof SkeletonHorseEntity horse && horse.isTrapped()) {
				horse.discard();
			}
			if (entity instanceof SpiderEntity spider) {
				spider.clearStatusEffects();
			}
		});

		// Creative item list: the client picked an item (or dropped one back to destroy it).
		ServerPlayNetworking.registerGlobalReceiver(SET_CREATIVE_CURSOR, (server, player, handler, buf, responseSender) -> {
			ItemStack stack = buf.readItemStack();
			server.execute(() -> {
				if (!player.isCreative()) return;
				if (!stack.isEmpty() && (stack.getCount() > stack.getMaxCount()
						|| !stack.isItemEnabled(player.getWorld().getEnabledFeatures()))) return;
				player.currentScreenHandler.setCursorStack(stack);
				player.currentScreenHandler.sendContentUpdates();
			});
		});

		LightningTorches.register();
		ThrowableBricks.register();
		GunpowderTrailBlock.register();
		MobSpawnEggItem.register();
		PackRules.register();

		LOGGER.info("SEJ initialized");
	}
}
