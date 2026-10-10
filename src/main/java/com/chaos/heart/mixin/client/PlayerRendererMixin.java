package com.chaos.heart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.chaos.heart.client.ChaosHeartRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

/** Dibuja "22❤" encima del nombre del avatar en lugar de debajo (como hace el scoreboard vanilla). */
@Mixin(AvatarRenderer.class)
public abstract class PlayerRendererMixin {

	/** Extrae los corazones y escala de latido al estado de renderizado del jugador. */
	@Inject(method = "extractRenderState",
			at = @At("TAIL"),
			remap = false)
	private void chaosheart$extractHearts(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		Scoreboard scoreboard = avatar.level().getScoreboard();
		Objective objective = scoreboard.getObjective(ChaosHeart.OBJECTIVE);
		if (objective != null && scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME) != objective) {
			// Servidor con el mod pero con showAboveName desactivado.
			return;
		}

		// Si el scoreText vanilla vino de nuestro objetivo de abajo del nombre, lo ocultamos de abajo.
		if (objective != null && scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME) == objective) {
			state.scoreText = null;
		}

		Component hearts;
		int value;
		ReadOnlyScoreInfo info = objective != null ? scoreboard.getPlayerScoreInfo(avatar, objective) : null;
		if (info != null) {
			hearts = info.formatValue(objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
			value = info.value();
		} else if (avatar instanceof LivingEntity living) {
			// Servidor sin el mod (o score aún no recibido): datos de entidad sincronizados.
			value = HeartText.value(living);
			hearts = HeartText.of(value);
		} else {
			return;
		}

		if (state instanceof ChaosHeartRenderState chState) {
			chState.chaosheart$setHearts(hearts);
			if (HeartText.isLow(value)) {
				chState.chaosheart$setBeatScale(HeartText.beatScale(avatar.tickCount + partialTick));
			} else {
				chState.chaosheart$setBeatScale(1.0F);
			}
		}
	}

	/** Dibuja los corazones arriba del nametag tras el render del nombre. */
	@Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("TAIL"),
			remap = false)
	private void chaosheart$renderHearts(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState cameraRenderState, CallbackInfo ci) {
		if (!(state instanceof ChaosHeartRenderState chState)) {
			return;
		}
		Component hearts = chState.chaosheart$getHearts();
		if (hearts == null || state.nameTagAttachment == null) {
			return;
		}

		poseStack.pushPose();
		int yOffset = state.showExtraEars ? -10 : 0;
		// Si se dibujó el nametag, subimos una línea (9 * 1.15 * 0.025) por encima.
		poseStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
		float scale = chState.chaosheart$getBeatScale();
		if (scale != 1.0F) {
			chaosheart$heartbeat(state, poseStack, scale);
		}
		collector.submitNameTag(poseStack, state.nameTagAttachment, yOffset, hearts, !state.isDiscrete, state.lightCoords, cameraRenderState);
		poseStack.popPose();
	}

	/** Escala la línea alrededor de su centro con un doble latido ("pum-pum") cada segundo. */
	@Unique
	private static void chaosheart$heartbeat(AvatarRenderState state, PoseStack poseStack, float scale) {
		Vec3 anchor = state.nameTagAttachment;
		if (anchor == null) {
			return;
		}
		double centerY = anchor.y + 0.5 - 4.5 * 0.025;
		poseStack.translate(anchor.x, centerY, anchor.z);
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-anchor.x, -centerY, -anchor.z);
	}
}
