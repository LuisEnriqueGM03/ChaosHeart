package com.chaos.heart.mixin.client;

import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

/** En el TAB, el ❤ de los jugadores con poca vida late igual que encima del nombre. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

	@Shadow(remap = false)
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "extractTablistScore",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"),
			remap = false)
	private void chaosheart$beatingHeart(GuiGraphicsExtractor extractor, Font font, Component text, int x, int y, int color,
			Operation<Void> original, @Local(argsOnly = true) Objective objective, @Local(argsOnly = true) UUID uuid) {
		if (!ChaosHeart.OBJECTIVE.equals(objective.getName()) || text.getSiblings().isEmpty()
				|| !HeartText.isLow(chaosheart$score(objective, uuid))) {
			original.call(extractor, font, text, x, y, color);
			return;
		}
		// "4❤" llega como literal "4" con el corazón de hermano: número fijo, corazón escalado sobre su centro.
		Component number = text.plainCopy().setStyle(text.getStyle());
		Component heart = text.getSiblings().get(0);
		original.call(extractor, font, number, x, y, color);

		int heartX = x + font.width(number);
		float scale = HeartText.beatScale(minecraft.gui.hud.getGuiTicks() + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
		float cx = heartX + font.width(heart) / 2.0F;
		float cy = y + 4.0F;

		extractor.pose().pushMatrix();
		extractor.pose().translate(cx, cy);
		extractor.pose().scale(scale, scale);
		extractor.pose().translate(-cx, -cy);
		original.call(extractor, font, heart, heartX, y, color);
		extractor.pose().popMatrix();
	}

	private int chaosheart$score(Objective objective, UUID uuid) {
		PlayerInfo info = minecraft.getConnection() != null ? minecraft.getConnection().getPlayerInfo(uuid) : null;
		if (info == null) {
			return 0;
		}
		ReadOnlyScoreInfo score = objective.getScoreboard()
				.getPlayerScoreInfo(ScoreHolder.forNameOnly(info.getProfile().name()), objective);
		return score != null ? score.value() : 0;
	}
}
