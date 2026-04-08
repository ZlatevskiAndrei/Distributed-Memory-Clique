package ids.alet_zlatevski.memory;


import java.util.HashMap;
import java.util.Map;

public class Memory {

    protected final int size;
    protected final long[] memory;
    protected final Map<Integer, AddressStatus> addressStatuses;
    public Memory(final int size) {
        this.size = size;
        this.memory = new long[size];
        this.addressStatuses = new HashMap<>();
        for (int i = 0; i < memory.length; i++) {
            this.addressStatuses.put(i, AddressStatus.FREE);
        }
    }

    public Map<Integer, AddressStatus> getAddressStatuses() {
        return addressStatuses;
    }
    public void setAddressStatus(int addr, AddressStatus status) {
        this.addressStatuses.replace(addr, status);
    }
    public void setValue(int addr, long value) {
        this.memory[addr] = value;
    }
    public long[] getMemorySnapshot() {
        return memory;
    }
}
