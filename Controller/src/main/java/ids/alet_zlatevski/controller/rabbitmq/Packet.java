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
        assert (this.type != null);

        this.data = new byte[buffer.remaining()];
        buffer.get(this.data);
    }

    public Packet(PacketType type) {
        this.type = type;
    }

    public Packet setData(String str) {
        this.data = str.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    public Packet setData(String nodeID, long LC) {
        return setData(nodeID + "," + LC);
    }

    public String getString() {
        return new String(this.data, StandardCharsets.UTF_8);
    }

    public IDAndLC getNodeIdAndLD() {
        String input = this.getString();

        int commaIndex = input.indexOf(',');
        String first = input.substring(0, commaIndex).trim();
        String second = input.substring(commaIndex + 1).trim();

        return new IDAndLC(first, Integer.parseInt(second));
    }

    public ID_ADDR_VAL getIdAddrVal() {
        String[] parts = this.getString().split(",");

        return new ID_ADDR_VAL(parts[0].trim(), Integer.parseInt(parts[1].trim()), Long.parseLong(parts[2].trim()));
    }


    public PacketType getType() {
        return type;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);

        buffer.putInt(this.type.ordinal());
        buffer.put(this.data);

        return buffer.array();
    }


    public record IDAndLC(String ID, int LD) {
    }
    public record ID_ADDR_VAL(String ID, int addr, long val) {
    }
}
