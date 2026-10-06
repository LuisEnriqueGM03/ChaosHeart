package com.chaos.heart.client;

import com.chaos.heart.config.HeartConfig;

import net.fabricmc.api.ClientModInitializer;

/** El render de los corazones vive en PlayerRendererMixin. */
public class ChaosHeartClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HeartConfig.load();
		if (SelfTest.enabled()) {
			SelfTest.register();
		}
	}
}
