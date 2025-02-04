package io.github.kosmx.emotes.main.network;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.impl.event.EventResult;
import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.api.events.client.ClientEmoteAPI;
import io.github.kosmx.emotes.api.events.client.ClientEmoteEvents;
import io.github.kosmx.emotes.api.PlayingAnimationData;
import io.github.kosmx.emotes.api.proxy.INetworkInstance;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.common.network.objects.NetData;
import io.github.kosmx.emotes.executor.EmoteInstance;
import io.github.kosmx.emotes.executor.emotePlayer.IEmotePlayerEntity;
import io.github.kosmx.emotes.inline.TmpGetters;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.config.ClientConfig;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ClientEmotePlay extends ClientEmoteAPI {

    /**
     * When the emotePacket arrives earlier than the player entity data
     * I put the emote into a queue.
     */
    //private static final int maxQueueLength = 256;
    private static final Map<UUID, PlayingAnimationData> queue = new ConcurrentHashMap<>();

    public static boolean clientStartLocalEmote(PlayingAnimationData data) {
        IEmotePlayerEntity player = TmpGetters.getClientMethods().getMainPlayer();
        if (player.emotecraft$isForcedEmote()) {
            return false;
        }

        ClientPacketManager.send(data.preparePacket(), null);
        ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(data, player.emotes_getUUID());
        TmpGetters.getClientMethods().getMainPlayer().emotecraft$playEmote(data);
        return true;
    }

    public static void clientRepeatLocalEmote(PlayingAnimationData data, UUID target) {
        EmotePacket.Builder packetBuilder = data.preparePacket().configureToStreamEmote(data.currentEmote(),
                TmpGetters.getClientMethods().getMainPlayer().emotes_getUUID()
        );
        ClientPacketManager.send(packetBuilder, target);
    }

    public static boolean clientStopLocalEmote() {
        if (TmpGetters.getClientMethods().getMainPlayer().isPlayingEmote()) {
            return clientStopLocalEmote(TmpGetters.getClientMethods().getMainPlayer().emotecraft$getEmote().getData());
        }
        return false;
    }

    public static boolean isForcedEmote() {
        IEmotePlayerEntity player = TmpGetters.getClientMethods().getMainPlayer();
        return player.emotecraft$isForcedEmote();
    }

    public static boolean clientStopLocalEmote(KeyframeAnimation emoteData) {
        if (emoteData != null && !TmpGetters.getClientMethods().getMainPlayer().emotecraft$isForcedEmote()) {
            EmotePacket.Builder packetBuilder = new EmotePacket.Builder();
            packetBuilder.configureToSendStop(emoteData.getUuid(), TmpGetters.getClientMethods().getMainPlayer().emotes_getUUID());
            ClientPacketManager.send(packetBuilder, null);
            TmpGetters.getClientMethods().getMainPlayer().stopEmote();

            ClientEmoteEvents.LOCAL_EMOTE_STOP.invoker().onEmoteStop();
            return true;
        }
        return false;
    }

    static void executeMessage(NetData data, INetworkInstance networkInstance) throws NullPointerException {
        EmoteInstance.instance.getLogger().log(Level.FINEST, "[emotes client] Received message: " + data);

        if (data.purpose == null) {
            if (EmoteInstance.config.showDebug.get()) {
                EmoteInstance.instance.getLogger().log(Level.INFO, "Packet execution is not possible without a purpose");
            }
        }
        switch (Objects.requireNonNull(data.purpose)) {
            case STREAM:
                assert data.emoteData != null;
                if(data.valid || !(((ClientConfig)EmoteInstance.config).alwaysValidate.get() || !networkInstance.safeProxy())) {
                    receivePlayPacket(data.player, new PlayingAnimationData(data));
                }
                break;
            case STOP:
                IEmotePlayerEntity player = PlatformTools.getPlayerFromUUID(data.player);
                assert data.stopEmoteID != null;
                if(player != null) {
                    ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(data.stopEmoteID, player.emotes_getUUID());
                    player.stopEmote(data.stopEmoteID);
                    if(player.isMainPlayer() && !data.isForced){
                        TmpGetters.getClientMethods().sendChatMessage(Component.translatable("emotecraft.blockedEmote"));
                    }
                }
                else {
                    queue.remove(data.player);
                }
                break;
            case CONFIG:
                networkInstance.setVersions(Objects.requireNonNull(data.versions));
                EmoteInstance.instance.getLogger().log(Level.INFO, "Legacy versions was received: " + data.versions, false);
                break;
            case FILE:
                EmoteHolder.addEmoteToList(data.emoteData).fromInstance = networkInstance;
            case UNKNOWN:
                if (EmoteInstance.config.showDebug.get()) {
                    EmoteInstance.instance.getLogger().log(Level.INFO, "Packet execution is not possible unknown purpose");
                }
                break;
        }
    }

    static void receivePlayPacket(UUID player, PlayingAnimationData data) {
        IEmotePlayerEntity playerEntity = PlatformTools.getPlayerFromUUID(player);
        if(isEmoteAllowed(data.currentEmote(), player)) {
            EventResult result = ClientEmoteEvents.EMOTE_VERIFICATION.invoker().verify(data, player);
            if (result == EventResult.FAIL) return;
            if (playerEntity != null) {
                ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(data, player);
                playerEntity.emotecraft$playEmote(data);
            }
            else {
                addToQueue(data, player);
            }
        }
    }

    public static boolean isEmoteAllowed(KeyframeAnimation emoteData, UUID player) {
        return (((ClientConfig)EmoteInstance.config).enablePlayerSafety.get() || !TmpGetters.getClientMethods().isPlayerBlocked(player))
                && (!emoteData.nsfw || ((ClientConfig)EmoteInstance.config).enableNSFW.get());
    }

    static void addToQueue(PlayingAnimationData entry, UUID player) {
        queue.put(player, entry);
    }


    /**
     * @param uuid get emote for this player
     * @return KeyframeAnimation, current tick of the emote
     */
    public static @Nullable PlayingAnimationData getEmoteForUUID(UUID uuid) {
        if (queue.containsKey(uuid)) {
            PlayingAnimationData entry = queue.remove(uuid);
            if (!entry.currentEmote().isPlayingAt(entry.calculatedTick(Instant.now())))
                return null;
            return entry;
        }
        return null;
    }

    /**
     * Call this periodically to keep the queue clean
     */
    public static void checkQueue(){
        for (var entry : ClientEmotePlay.queue.entrySet()) {
            int currentTick = entry.getValue().calculatedTick(Instant.now());
            if (!entry.getValue().currentEmote().isPlayingAt(currentTick)) {
                ClientEmotePlay.queue.remove(entry.getKey());
            }
        }
    }

    public static void init() {
        ClientEmoteAPI.INSTANCE = new ClientEmotePlay();
    }

    @Override
    protected boolean playEmoteImpl(PlayingAnimationData animation) {
        if (animation != null) {
            return clientStartLocalEmote(animation);
        } else {
            return clientStopLocalEmote();
        }
    }

    @Override
    protected Collection<KeyframeAnimation> clientEmoteListImpl() {
        return EmoteHolder.list.values().stream().map(EmoteHolder::getEmote).collect(Collectors.toList());
    }
}
