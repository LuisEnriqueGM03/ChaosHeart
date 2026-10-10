package com.chaos.heart.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.chaos.heart.ChaosHeart;
import com.chaos.heart.HeartText;
import com.chaos.heart.config.HeartConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;

/** En el TAB, el ❤ de los jugadores con poca vida late igual que encima del nombre. */
@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "renderTablistScore",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
	private int chaosheart$beatingHeart(GuiGraphics graphics, Font font, String text, int x, int y, int color,
			Operation<Integer> original, @Local(argsOnly = true) Objective objective, @Local(argsOnly = true) String name) {
		if (objective == null || !ChaosHeart.OBJECTIVE.equals(objective.getName())) {
			return original.call(graphics, font, text, x, y, color);
		}
		int score = objective.getScoreboard().getOrCreatePlayerScore(name, objective).getScore();
		Component number = Component.literal(Integer.toString(score)).withStyle(ChatFormatting.WHITE);
		Component heart = Component.literal(HeartConfig.get().symbol)
				.withStyle(score > 20 ? ChatFormatting.GOLD : ChatFormatting.RED);

		int endX = x + font.width(text);
		int heartWidth = font.width(heart);
		int numWidth = font.width(number);
		int startX = endX - numWidth - heartWidth;

		graphics.drawString(font, number, startX, y, color);

		int heartX = startX + numWidth;
		if (HeartText.isLow(score)) {
			float partialTick = minecraft.getFrameTime();
			float scale = HeartText.beatScale(minecraft.gui.getGuiTicks() + partialTick);
			float cx = heartX + heartWidth / 2.0F;
			float cy = y + 4.0F;
			graphics.pose().pushPose();
			graphics.pose().translate(cx, cy, 0.0F);
			graphics.pose().scale(scale, scale, 1.0F);
			graphics.pose().translate(-cx, -cy, 0.0F);
			graphics.drawString(font, heart, heartX, y, color);
			graphics.pose().popPose();
		} else {
			graphics.drawString(font, heart, heartX, y, color);
		}
		return endX;
	}
}
