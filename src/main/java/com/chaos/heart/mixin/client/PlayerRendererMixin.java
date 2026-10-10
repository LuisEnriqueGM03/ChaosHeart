package com.chaos.heart.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/** Dibuja "22❤" encima del nombre del jugador en lugar de debajo (como hace el scoreboard vanilla). */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

	/** Oculta el render vanilla de nuestro objetivo bajo el nombre; otros objetivos siguen igual. */
	@WrapOperation(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;getDisplayObjective(I)Lnet/minecraft/world/scores/Objective;"))
	private Objective chaosheart$hideVanillaBelowName(Scoreboard scoreboard, int slot, Operation<Objective> original) {
		Objective objective = original.call(scoreboard, slot);
		return objective != null && ChaosHeart.OBJECTIVE.equals(objective.getName()) ? null : objective;
	}

	/** Tras dibujar el nombre, sube una línea y dibuja los corazones. */
	@WrapOperation(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At(value = "INVOKE", ordinal = 1,
					target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
	private void chaosheart$renderHearts(PlayerRenderer renderer, Entity entity, Component name, PoseStack poseStack,
			MultiBufferSource buffer, int light, Operation<Void> original) {
		original.call(renderer, entity, name, poseStack, buffer, light);
		if (!(entity instanceof AbstractClientPlayer player)) {
			return;
		}
		Scoreboard scoreboard = player.getScoreboard();
		Objective objective = scoreboard.getObjective(ChaosHeart.OBJECTIVE);
		if (objective != null && scoreboard.getDisplayObjective(Scoreboard.DISPLAY_SLOT_BELOW_NAME) != objective) {
			// Servidor con el mod pero con showAboveName desactivado.
			return;
		}
		int value;
		if (objective != null && scoreboard.hasPlayerScore(player.getScoreboardName(), objective)) {
			value = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective).getScore();
		} else {
			// Servidor sin el mod (o score aún no recibido): datos de entidad sincronizados.
			value = HeartText.value(player);
		}
		Component hearts = HeartText.of(value);

		poseStack.pushPose();
		poseStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
		if (HeartText.isLow(value)) {
			chaosheart$heartbeat(player, poseStack);
		}
		original.call(renderer, entity, hearts, poseStack, buffer, light);
		poseStack.popPose();
	}

	/** Escala la línea alrededor de su centro con un doble latido ("pum-pum") cada segundo. */
	private static void chaosheart$heartbeat(AbstractClientPlayer player, PoseStack poseStack) {
		float partialTick = Minecraft.getInstance().getFrameTime();
		float scale = HeartText.beatScale(player.tickCount + partialTick);
		float anchorY = player.getNameTagOffsetY();
		double centerY = anchorY + 0.5 - 4.5 * 0.025;
		poseStack.translate(0.0, centerY, 0.0);
		poseStack.scale(scale, scale, scale);
		poseStack.translate(0.0, -centerY, 0.0);
	}
}
