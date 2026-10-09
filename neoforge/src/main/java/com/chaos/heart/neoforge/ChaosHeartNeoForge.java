package com.chaos.heart.neoforge;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.config.HeartConfig;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ChaosHeart.MOD_ID)
public class ChaosHeartNeoForge {

	public ChaosHeartNeoForge(IEventBus modEventBus) {
		HeartConfig.load(FMLPaths.CONFIGDIR.get());

		IEventBus forgeBus = NeoForge.EVENT_BUS;
		forgeBus.addListener(ServerStartedEvent.class, event -> ChaosHeart.setupObjective(event.getServer()));
		forgeBus.addListener(ServerStoppedEvent.class, event -> ChaosHeart.clear());
		forgeBus.addListener(ServerTickEvent.Post.class, event -> ChaosHeart.tick(event.getServer()));
		forgeBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				ChaosHeart.handleDisconnect(player, player.getServer());
			}
		});

		if (FMLEnvironment.dist == Dist.CLIENT && SelfTestNeoForge.enabled()) {
			SelfTestNeoForge.register();
		}
	}
}
