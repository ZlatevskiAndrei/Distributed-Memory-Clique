package ids.alet_zlatevski.controller.rabbitmq;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Packet {

    protected PacketType type;

    protected byte[] data;

    public Packet(final byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);

        int typeOrdinal = buffer.getInt();
        this.type = PacketType.values()[typeOrdinal];

        this.data = new byte[buffer.remaining()];
        buffer.get(this.data);
    }

    public Packet(PacketType type) {
        this.type = type;
    }

    public void setData(String str) {
        this.data = str.getBytes(StandardCharsets.UTF_8);
    }

    public String getString() {
        return new String(this.data, StandardCharsets.UTF_8);
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);

        buffer.putInt(this.type.ordinal());
        buffer.put(this.data);

        return buffer.array();
    }


}
