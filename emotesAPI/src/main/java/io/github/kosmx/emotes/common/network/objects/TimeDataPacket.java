package io.github.kosmx.emotes.common.network.objects;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TimeDataPacket extends AbstractNetworkPacket {
    @Override
    public byte getID() {
        return 55;
    }

    @Override
    public byte getVer() {
        return 0;
    }

    @Override
    public boolean read(ByteBuffer byteBuffer, NetData config, int version) throws IOException {
        config.startTime = byteBuffer.getLong();
        return true;
    }

    @Override
    public void write(ByteBuffer byteBuffer, NetData config) throws IOException {
        byteBuffer.putLong(config.startTime);
    }

    @Override
    public boolean doWrite(NetData config) {
        return config.startTime > 0;
    }

    @Override
    public int calculateSize(NetData config) {
        return 8;
    }
}
