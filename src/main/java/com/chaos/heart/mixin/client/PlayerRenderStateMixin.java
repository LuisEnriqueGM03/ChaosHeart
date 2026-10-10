package com.chaos.heart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.chaos.heart.client.ChaosHeartRenderState;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;

@Mixin(AvatarRenderState.class)
public class PlayerRenderStateMixin implements ChaosHeartRenderState {
	@Unique
	private Component chaosheart$hearts;
	@Unique
	private float chaosheart$beatScale = 1.0F;

	@Override
	public Component chaosheart$getHearts() {
		return chaosheart$hearts;
	}

	@Override
	public void chaosheart$setHearts(Component hearts) {
		this.chaosheart$hearts = hearts;
	}

	@Override
	public float chaosheart$getBeatScale() {
		return chaosheart$beatScale;
	}

	@Override
	public void chaosheart$setBeatScale(float scale) {
		this.chaosheart$beatScale = scale;
	}
}
