package com.chaos.heart.fabric;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.config.HeartConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public class ChaosHeartFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		HeartConfig.load(FabricLoader.getInstance().getConfigDir());

		ServerLifecycleEvents.SERVER_STARTED.register(ChaosHeart::setupObjective);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChaosHeart.clear());
		ServerTickEvents.END_SERVER_TICK.register(ChaosHeart::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ChaosHeart.handleDisconnect(handler.getPlayer(), server);
		});
	}
}
