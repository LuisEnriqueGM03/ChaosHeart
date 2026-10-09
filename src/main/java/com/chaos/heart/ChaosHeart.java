package com.chaos.heart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chaos.heart.config.HeartConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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

		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.addListener(this::onServerStarted);
		forgeBus.addListener(this::onServerStopped);
		forgeBus.addListener(this::onServerTick);
		forgeBus.addListener(this::onPlayerLoggedOut);
	}

	private void onServerStarted(ServerStartedEvent event) {
		setupObjective(event.getServer());
	}

	private void onServerStopped(ServerStoppedEvent event) {
		LAST.clear();
	}

	private void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			tick(event.getServer());
		}
	}

	private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			handleDisconnect(player, player.getServer());
		}
	}

	public static void setupObjective(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(OBJECTIVE);
		if (objective == null) {
			objective = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, Component.empty(),
					ObjectiveCriteria.RenderType.INTEGER, false, null);
		}
		HeartConfig config = HeartConfig.get();
		if (config.showAboveName) {
			scoreboard.setDisplayObjective(DisplaySlot.BELOW_NAME, objective);
		} else if (scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME) == objective) {
			scoreboard.setDisplayObjective(DisplaySlot.BELOW_NAME, null);
		}
		if (config.showInTab) {
			scoreboard.setDisplayObjective(DisplaySlot.LIST, objective);
		} else if (scoreboard.getDisplayObjective(DisplaySlot.LIST) == objective) {
			scoreboard.setDisplayObjective(DisplaySlot.LIST, null);
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
			ScoreAccess score = scoreboard.getOrCreatePlayerScore(player, objective);
			score.numberFormatOverride(new FixedFormat(HeartText.of(value)));
			score.set(value);
		}
	}

	public static void handleDisconnect(ServerPlayer player, MinecraftServer server) {
		LAST.remove(player.getUUID());
		if (server != null) {
			Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
			if (objective != null) {
				server.getScoreboard().resetSinglePlayerScore(player, objective);
			}
		}
	}
}
