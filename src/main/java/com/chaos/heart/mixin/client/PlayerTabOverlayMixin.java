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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;

/** En el TAB, el ❤ de los jugadores con poca vida late igual que encima del nombre. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "renderTablistScore",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
	private int chaosheart$beatingHeart(GuiGraphics graphics, Font font, Component text, int x, int y, int color,
			Operation<Integer> original, @Local(argsOnly = true) Objective objective, @Local(argsOnly = true) UUID uuid) {
		if (!ChaosHeart.OBJECTIVE.equals(objective.getName()) || text.getSiblings().isEmpty()
				|| !HeartText.isLow(chaosheart$score(objective, uuid))) {
			return original.call(graphics, font, text, x, y, color);
		}
		// "4❤" llega como literal "4" con el corazón de hermano: número fijo, corazón escalado sobre su centro.
		Component number = text.plainCopy().setStyle(text.getStyle());
		Component heart = text.getSiblings().get(0);
		int heartX = original.call(graphics, font, number, x, y, color);
		float scale = HeartText.beatScale(minecraft.gui.getGuiTicks() + minecraft.getTimer().getGameTimeDeltaPartialTick(false));
		float cx = heartX + font.width(heart) / 2.0F;
		float cy = y + 4.0F;
		graphics.pose().pushPose();
		graphics.pose().translate(cx, cy, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-cx, -cy, 0.0F);
		int end = original.call(graphics, font, heart, heartX, y, color);
		graphics.pose().popPose();
		return end;
	}

	private int chaosheart$score(Objective objective, UUID uuid) {
		PlayerInfo info = minecraft.getConnection() != null ? minecraft.getConnection().getPlayerInfo(uuid) : null;
		if (info == null) {
			return 0;
		}
		ReadOnlyScoreInfo score = objective.getScoreboard()
				.getPlayerScoreInfo(ScoreHolder.forNameOnly(info.getProfile().getName()), objective);
		return score != null ? score.value() : 0;
	}
}
