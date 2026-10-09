package com.chaos.heart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.chaos.heart.client.ChaosHeartRenderState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

/** Dibuja "22❤" encima del nombre del jugador en lugar de debajo (como hace el scoreboard vanilla). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

	/** Oculta el render vanilla de nuestro objetivo bajo el nombre; otros objetivos siguen igual. */
	@WrapOperation(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;getDisplayObjective(Lnet/minecraft/world/scores/DisplaySlot;)Lnet/minecraft/world/scores/Objective;"))
	private Objective chaosheart$hideVanillaBelowName(Scoreboard scoreboard, DisplaySlot slot, Operation<Objective> original) {
		Objective objective = original.call(scoreboard, slot);
		return objective != null && ChaosHeart.OBJECTIVE.equals(objective.getName()) ? null : objective;
	}

	/** Extrae los corazones y escala de latido al estado de renderizado del jugador. */
	@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
			at = @At("TAIL"))
	private void chaosheart$extractHearts(AbstractClientPlayer player, PlayerRenderState state, float partialTick, CallbackInfo ci) {
		Scoreboard scoreboard = player.getScoreboard();
		Objective objective = scoreboard.getObjective(ChaosHeart.OBJECTIVE);
		if (objective != null && scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME) != objective) {
			// Servidor con el mod pero con showAboveName desactivado.
			return;
		}
		Component hearts;
		int value;
		ReadOnlyScoreInfo info = objective != null ? scoreboard.getPlayerScoreInfo(player, objective) : null;
		if (info != null) {
			hearts = ReadOnlyScoreInfo.safeFormatValue(info, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
			value = info.value();
		} else {
			// Servidor sin el mod (o score aún no recibido): datos de entidad sincronizados.
			value = HeartText.value(player);
			hearts = HeartText.of(value);
		}

		if (state instanceof ChaosHeartRenderState chState) {
			chState.chaosheart$setHearts(hearts);
			if (HeartText.isLow(value)) {
				chState.chaosheart$setBeatScale(HeartText.beatScale(player.tickCount + partialTick));
			} else {
				chState.chaosheart$setBeatScale(1.0F);
			}
		}
	}

	/** Tras dibujar el nombre, sube una línea y dibuja los corazones. */
	@WrapOperation(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", ordinal = 1,
					target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void chaosheart$renderHearts(PlayerRenderer renderer, EntityRenderState entityState, Component name, PoseStack poseStack,
			MultiBufferSource buffer, int light, Operation<Void> original) {
		original.call(renderer, entityState, name, poseStack, buffer, light);
		if (!(entityState instanceof ChaosHeartRenderState chState)) {
			return;
		}
		Component hearts = chState.chaosheart$getHearts();
		if (hearts == null) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
		float scale = chState.chaosheart$getBeatScale();
		if (scale != 1.0F) {
			chaosheart$heartbeat(entityState, poseStack, scale);
		}
		original.call(renderer, entityState, hearts, poseStack, buffer, light);
		poseStack.popPose();
	}

	/** Escala la línea alrededor de su centro con un doble latido ("pum-pum") cada segundo. */
	private static void chaosheart$heartbeat(EntityRenderState state, PoseStack poseStack, float scale) {
		// Mismo anclaje que EntityRenderer.renderNameTag; el texto ocupa 9px hacia abajo desde ahí.
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
