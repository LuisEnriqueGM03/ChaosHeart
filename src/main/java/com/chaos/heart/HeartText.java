package com.chaos.heart;

import com.chaos.heart.config.HeartConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

/** Texto "22❤": vida + absorción en puntos de vida (20 = vida completa). */
public final class HeartText {

	private HeartText() {
	}

	/** Vida + absorción redondeada hacia arriba, como el criterio "health" vanilla. */
	public static int value(LivingEntity entity) {
		if (entity.isDeadOrDying()) {
			return 0;
		}
		return (int) Math.ceil(entity.getHealth() + entity.getAbsorptionAmount());
	}

	/** Con 2 corazones (4 de vida) o menos el corazón late. */
	public static boolean isLow(int value) {
		return value > 0 && value <= 4;
	}

	/** Escala del latido: doble pulso ("pum-pum") cada segundo; time en ticks. */
	public static float beatScale(float time) {
		float phase = (time % 20.0F) / 20.0F;
		double beat = Math.exp(-Math.pow((phase - 0.10F) / 0.05F, 2))
				+ 0.6F * Math.exp(-Math.pow((phase - 0.30F) / 0.05F, 2));
		return 1.0F + 0.35F * (float) beat;
	}

	/** Número en blanco seguido del corazón: rojo, o dorado por encima de 20. */
	public static MutableComponent of(int value) {
		return Component.literal(Integer.toString(value))
				.withStyle(ChatFormatting.WHITE)
				.append(Component.literal(HeartConfig.get().symbol)
						.withStyle(value > 20 ? ChatFormatting.GOLD : ChatFormatting.RED));
	}

	public static MutableComponent of(LivingEntity entity) {
		return of(value(entity));
	}
}
