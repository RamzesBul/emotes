package io.github.kosmx.emotes.api;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.common.network.EmotePacket;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;

public record PlayingAnimationData(KeyframeAnimation currentEmote, int tick, Instant startTime, boolean forced) {
    public EmotePacket.Builder preparePacket() {
        return new EmotePacket.Builder()
                .configureToStreamEmote(currentEmote())
                .setStartTime(startTime())
                .configureEmoteTick(tick());
    }

    public int calculatedTick(Instant now) {
        return PlayingAnimationData.calculateTick(startTime(), now) + this.tick;
    }

    public static int calculateTick(Temporal startTime, Temporal newStartTime) {
        int between = (int) Duration.between(startTime, newStartTime).toMillis();
        System.out.println("between " + between / 50);
        return between / 50;
    }
}
