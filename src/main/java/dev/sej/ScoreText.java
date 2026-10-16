package dev.sej;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ScoreText {

    private ScoreText() {}

    public static Text of(PlayerEntity player) {
        int score = player.getScore();
        int high = ((Sej.HighScoreHolder) player).sej$getHighScore();
        boolean record = score >= high;

        MutableText text = Text.translatable("deathScreen.score")
                .append(": ")
                .append(Text.literal(String.valueOf(score))
                        .formatted(record ? Formatting.GOLD : Formatting.YELLOW));

        if (!record) {
            text.append(Text.literal(" (" + high + ")").formatted(Formatting.GRAY));
        }
        return text;
    }
}