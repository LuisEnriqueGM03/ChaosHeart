package com.chaos.heart.client;

import net.minecraft.network.chat.Component;

public interface ChaosHeartRenderState {
	Component chaosheart$getHearts();
	void chaosheart$setHearts(Component hearts);
	float chaosheart$getBeatScale();
	void chaosheart$setBeatScale(float scale);
}
