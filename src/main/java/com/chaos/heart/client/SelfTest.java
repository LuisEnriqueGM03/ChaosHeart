package com.chaos.heart.client;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;

/**
 * Autoprueba visual, solo en desarrollo (./gradlew runSelftest).
 * Crea un segundo jugador ("ChaosFriend") delante de la cámara y fotografía cada caso:
 * nametag normal, dorado (>20), con corazones extra (40) y latiendo con poca vida, y el TAB.
 * Las capturas quedan en run/screenshots/selftest_*.png; las series "*_fNN" son fotogramas de un latido.
 */
public final class SelfTest {

	private static final int START = 60;
	private static final int SCENE = 40;
	private static final int FRAMES = 20;
	private static final GameProfile FRIEND = new GameProfile(UUID.fromString("6c1e2a4b-3d5f-4a7b-9c8d-0e1f2a3b4c5d"), "ChaosFriend");

	private static int ticks = -1;
	private static FakePlayer friend;
	/** Posición fija de la cámara, la elige el servidor en setup(). */
	private static volatile Vec3 camera;

	private SelfTest() {}

	public static boolean enabled() {
		return System.getProperty("chaosheart.selftest") != null;
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
			return;
		}
		mc.options.pauseOnLostFocus = false;
		mc.gui.toastManager().clear();
		ticks++;
		if (ticks == START) {
			onServer(mc, SelfTest::setup);
		}
		if (camera != null) {
			// Se fija cada tick: el ratón real movería la cámara entre medias. Mira al norte, hacia el amigo.
			mc.player.setPos(camera);
			mc.player.setDeltaMovement(Vec3.ZERO);
			mc.player.setYRot(180.0F);
			mc.player.setXRot(-8.0F);
		}

		// Margen para que se vuelvan a dibujar los chunks tras despejar la escena.
		int t = ticks - START - 60;

		// 1. Nametag con vida completa.
		if (t == SCENE) {
			shot(mc, "01_nametag");
			onFriend(mc, p -> p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 6000, 0)));
		}
		// 2. Con absorción: más de 20, corazón dorado.
		if (t == SCENE * 2) {
			shot(mc, "02_nametag_gold");
			onFriend(mc, p -> {
				p.removeEffect(MobEffects.ABSORPTION);
				p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
				p.setHealth(40.0F);
			});
		}
		// 3. Corazones extra (max_health 40), como los que dan otros mods.
		if (t == SCENE * 3) {
			shot(mc, "03_nametag_extra_hearts");
			onFriend(mc, p -> {
				p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
				p.setHealth(3.0F);
			});
		}
		// 4. Poca vida: el nametag late.
		frames(mc, t, SCENE * 4, "04_nametag_low");

		// 5. TAB: tú con 20 y el amigo con 3 latiendo.
		if (t == SCENE * 5 - 10) {
			mc.options.keyPlayerList.setDown(true);
		}
		if (t == SCENE * 5) {
			shot(mc, "05_tab");
		}
		frames(mc, t, SCENE * 5 + 2, "05_tab_low");

		// 6. TAB con tu absorción (dorado) y el nametag del amigo latiendo a la vez.
		if (t == SCENE * 6) {
			onServer(mc, server -> me(mc, server).addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 6000, 1)));
		}
		if (t == SCENE * 7) {
			shot(mc, "06_tab_gold");
		}
		if (t == SCENE * 7 + 10) {
			ChaosHeart.LOGGER.info("[selftest] terminado");
			mc.stop();
		}
	}

	/** Una captura por tick durante un latido completo. */
	private static void frames(Minecraft mc, int t, int start, String name) {
		int i = t - start;
		if (i >= 0 && i < FRAMES) {
			shot(mc, String.format("%s_f%02d", name, i));
		}
	}

	private static void setup(MinecraftServer server) {
		ServerLevel level = server.overworld();
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear 6000");

		BlockPos base = level.getRespawnData().pos().offset(120, 0, 120);
		level.getChunk(base);
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
		clearScene(level, ground);

		ServerPlayer me = server.getPlayerList().getPlayers().get(0);
		me.setGameMode(GameType.SURVIVAL);
		me.setHealth(me.getMaxHealth());
		me.getFoodData().setFoodLevel(20);
		// La cámara, 4 bloques al sur del amigo, mirando al norte (yaw 180).
		me.teleportTo(level, ground.getX() + 0.5, ground.getY(), ground.getZ() + 4.5, Set.of(), 180.0F, -8.0F, false);
		camera = me.position();

		friend = FakePlayer.get(level, FRIEND);
		friend.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
		friend.setYHeadRot(0.0F);
		friend.setHealth(20.0F);
		server.getPlayerList().broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(friend)));
		level.addNewPlayer(friend);
		updateFriendScore(server);
	}

	/** Explanada de hierba sin árboles ni agua alrededor de la escena. */
	private static void clearScene(ServerLevel level, BlockPos ground) {
		for (BlockPos pos : BlockPos.betweenClosed(ground.offset(-12, -3, -10), ground.offset(12, 20, 12))) {
			int dy = pos.getY() - ground.getY();
			BlockState state = dy >= 0 ? Blocks.AIR.defaultBlockState()
					: dy == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		}
	}

	/** El amigo no está en la lista de jugadores, así que el tick del mod no le pone score: se hace aquí. */
	private static void updateFriendScore(MinecraftServer server) {
		Objective objective = server.getScoreboard().getObjective(ChaosHeart.OBJECTIVE);
		if (objective == null || friend == null) {
			return;
		}
		int value = HeartText.value(friend);
		ScoreAccess score = server.getScoreboard().getOrCreatePlayerScore(friend, objective);
		score.numberFormatOverride(new FixedFormat(HeartText.of(value)));
		score.set(value);
	}

	private static void onFriend(Minecraft mc, Consumer<ServerPlayer> action) {
		onServer(mc, server -> {
			if (friend != null) {
				action.accept(friend);
				updateFriendScore(server);
			}
		});
	}

	private static ServerPlayer me(Minecraft mc, MinecraftServer server) {
		return server.getPlayerList().getPlayer(mc.player.getUUID());
	}

	private static void onServer(Minecraft mc, Consumer<MinecraftServer> action) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> action.accept(server));
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
				msg -> ChaosHeart.LOGGER.info("[selftest] {}", msg.getString()));
	}
}
