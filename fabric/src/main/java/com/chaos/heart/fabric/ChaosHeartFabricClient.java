package com.chaos.heart.fabric;

import com.chaos.heart.config.HeartConfig;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** El render de los corazones vive en PlayerRendererMixin (módulo common). */
public class ChaosHeartFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HeartConfig.load(FabricLoader.getInstance().getConfigDir());
		if (SelfTest.enabled()) {
			SelfTest.register();
		}
	}
}
