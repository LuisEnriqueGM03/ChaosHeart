package com.chaos.heart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chaos.heart.config.HeartConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(ChaosHeart.MOD_ID)
public class ChaosHeart {

	public static final String MOD_ID = "chaosheart";
	public static final String OBJECTIVE = "chaosheart";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Map<UUID, Integer> LAST = new HashMap<>();

	public ChaosHeart() {
		HeartConfig.load(FMLPaths.CONFIGDIR.get());
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		setupObjective(event.getServer());
	}

	@SubscribeEvent
	public void onServerStopped(ServerStoppedEvent event) {
		LAST.clear();
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			tick(event.getServer());
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			handleDisconnect(player, player.getServer());
		}
	}

	public static void setupObjective(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(OBJECTIVE);
		if (objective == null) {
			objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, Component.empty(),
					ObjectiveCriteria.RenderType.INTEGER);
		}
		HeartConfig config = HeartConfig.get();
		if (config.showAboveName) {
			scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_BELOW_NAME, objective);
		} else if (scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_BELOW_NAME) == objective) {
			scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_BELOW_NAME, null);
		}
		if (config.showInTab) {
			scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST, objective);
		} else if (scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST) == objective) {
			scoreboard.setDisplayObjective(Scoreboard.DISPLAY_SLOT_LIST, null);
		}
	}

	public static void tick(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(OBJECTIVE);
		if (objective == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int value = HeartText.value(player);
			Integer previous = LAST.put(player.getUUID(), value);
			if (previous != null && previous == value) {
				continue;
			}
			Score score = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);
			score.setScore(value);
		}
	}

	public static void handleDisconnect(ServerPlayer player, MinecraftServer server) {
		LAST.remove(player.getUUID());
		if (server != null) {
			Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
			if (objective != null) {
				server.getScoreboard().resetPlayerScore(player.getScoreboardName(), objective);
			}
		}
	}
}
