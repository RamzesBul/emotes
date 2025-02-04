package io.github.kosmx.emotes.server.network;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

import io.github.kosmx.emotes.api.PlayingAnimationData;
import org.jetbrains.annotations.Nullable;
import java.time.Instant;

/**
 * Server side emote state tracking
 * It uses {@link Instant}
 * By using instant, tracking is mostly immune to server lags, tick drops
 * However susceptible to system clock changes.
 * And less demanding for a large server
 *
 */
public class EmotePlayTracker {

    private KeyframeAnimation currentEmote = null;
    private int tick;

    private Instant startTime = null;

    private boolean isForced = false;

    public void removePlayedEmote() {
        setPlayedEmote(null, 0, null, false);
    }

    /**
     * Set the currently played emote.
     * @param data Emote, null if stop playing
     */
    public void setPlayedEmote(@Nullable KeyframeAnimation data, int tick, @Nullable Instant startTime, boolean isForced) {
        this.currentEmote = data;
        this.tick = tick;
        if (data == null) {
            this.startTime = null;
            this.isForced = false;
        } else {
            if (startTime == null || !isPlayingAt(startTime)) {
                startTime = Instant.now();
            }
            this.startTime = startTime;
            this.isForced = isForced;
        }
    }

    /**
     * Is the currently played emote forced
     * Returns false if not playing emote
     * a.k.a. disallow the user play a different emote
     * @return true if forced, false if not playing any emote.
     */
    public boolean isForced() {
        if( getPlayedEmote() != null) {
            return isForced;
        }
        else return false;
    }

    /**
     * Get the currently played emote and the tick time
     * @return null if not playing emote
     */
    @Nullable
    public PlayingAnimationData getPlayedEmote() {
        if (currentEmote == null) return null;
        Instant newStartTime = Instant.now();
        int tick = PlayingAnimationData.calculateTick(startTime, newStartTime) + this.tick;
        if (!currentEmote.isPlayingAt(tick)) {
            currentEmote = null;
            startTime = null;
            isForced = false;
            return null;
        }
        return new PlayingAnimationData(this.currentEmote, tick, newStartTime, this.isForced);
    }

    public boolean isPlayingAt(Instant instant) {
        if (this.currentEmote == null) {
            return false;
        }
        int tick = PlayingAnimationData.calculateTick(instant, Instant.now()) + this.tick;
        return this.currentEmote.isPlayingAt(tick);
    }
}
