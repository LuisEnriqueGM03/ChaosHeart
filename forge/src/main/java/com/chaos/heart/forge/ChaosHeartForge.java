package com.chaos.heart.forge;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.config.HeartConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(ChaosHeart.MOD_ID)
public class ChaosHeartForge {

	public ChaosHeartForge() {
		HeartConfig.load(FMLPaths.CONFIGDIR.get());

		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.addListener(this::onServerStarted);
		forgeBus.addListener(this::onServerStopped);
		forgeBus.addListener(this::onServerTick);
		forgeBus.addListener(this::onPlayerLoggedOut);
	}

	private void onServerStarted(ServerStartedEvent event) {
		ChaosHeart.setupObjective(event.getServer());
	}

	private void onServerStopped(ServerStoppedEvent event) {
		ChaosHeart.clear();
	}

	private void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			ChaosHeart.tick(event.getServer());
		}
	}

	private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			ChaosHeart.handleDisconnect(player, player.getServer());
		}
	}
}
