package io.github.kosmx.emotes.api.events.client;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;

import io.github.kosmx.emotes.api.PlayingAnimationData;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

public abstract class ClientEmoteAPI {
    /**
     * Stop play an emote.
     */
    public static boolean stopEmote() {
        return ClientEmoteAPI.playEmote(null);
    }

    /**
     * Start playing an emote.
     * @param data animation data, <code>null</code> to stop playing.
     * @return Can the emote be played: this doesn't check server-side verification
     */
    public static boolean playEmote(@Nullable PlayingAnimationData data) {
        return INSTANCE.playEmoteImpl(data);
    }

    /**
     * A list of client-side active emotes.
     * You can not modify the list.
     * @return Client-side active emotes
     */
    public static Collection<KeyframeAnimation> clientEmoteList() {
        return INSTANCE.clientEmoteListImpl();
    }

    // ---- IMPLEMENTATION ---- //

    protected static ClientEmoteAPI INSTANCE;

    protected abstract boolean playEmoteImpl(PlayingAnimationData data);

    protected abstract Collection<KeyframeAnimation> clientEmoteListImpl();
}
