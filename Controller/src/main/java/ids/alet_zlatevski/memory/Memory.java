package ids.alet_zlatevski.memory;


import java.util.HashMap;
import java.util.Map;

public class Memory {

    protected final int size;
    protected final long[] memory;
    /**
     * TODO we have here the concrete example of that we are not consistent in our vision of
     *  what we lock. As here the memory has a "by address granularity" where our mutualExclusionHandler has a
     *  global memory block this.mutualExclusionHandler.initiateWantLock() we lock the whole memory.
     *  For now at maximum the addressStatuses can contain only one address with the BUSY status, so
     *  the haspmap is "useless".
     *
     *  We have 2 choices :
     *  - set the granularity of the memory to only one write at time (removing the hashap, replacing
     *  it by an address, addressStatus. (easy and simple)
     *  - set the granularity of the mutualExclusionHandler to "by address".
     *  (need more changes, may be a bit tricky to test.
     *
     *
     *  My proposition first go with the simple granularity of the memory, if we finish early we may introduce
     *  the granularity for the mutualExclusionHandler. For now don't touch to anything this is not
     *  a blocking issue we will discuss about it.
     */
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
