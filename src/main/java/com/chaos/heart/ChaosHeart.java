package com.chaos.heart;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chaos.heart.config.HeartConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Mantiene el objetivo de scoreboard "chaosheart" con la vida de cada jugador como valor numérico.
 * El scoreboard se sincroniza a todos los clientes, así el TAB funciona incluso en clientes vanilla.
 */
public class ChaosHeart implements ModInitializer {

	public static final String MOD_ID = "chaosheart";
	public static final String OBJECTIVE = "chaosheart";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Último valor enviado por jugador. */
	private static final Map<UUID, Integer> LAST = new HashMap<>();

	@Override
	public void onInitialize() {
		HeartConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(ChaosHeart::setupObjective);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST.clear());
		ServerTickEvents.END_SERVER_TICK.register(ChaosHeart::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			LAST.remove(player.getUUID());
			Objective objective = server.getScoreboard().getObjective(OBJECTIVE);
			if (objective != null) {
				server.getScoreboard().resetPlayerScore(player.getScoreboardName(), objective);
			}
		});
	}

	private static void setupObjective(MinecraftServer server) {
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

	private static void tick(MinecraftServer server) {
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
}
