package ids.alet_zlatevski.memory;

import ids.alet_zlatevski.controller.rabbitmq.MutualExclusionHandler;

import java.util.HashMap;
import java.util.Map;

public class Memory {

    protected final int size;
    protected final int[] memory;

    protected MutualExclusionHandler mutualExclusionHandler; // TODO may be usefull ?

     protected final Map<AddressStatus, Long> addressStatuses;
    public Memory(final int size, final MutualExclusionHandler mutualExclusionHandler) {
        this.size = size;
        this.memory = new int[size];
        this.mutualExclusionHandler = mutualExclusionHandler;
        this.addressStatuses = new HashMap<>();
        for(long i: memory) { this.addressStatuses.put(AddressStatus.FREE, i); }
    }

    public void read(long addr) {
        // TODO care about synchronization when reading, becareful to check if the memory is lock or not in the mutualExclusionHandler.
    }

    public void write(long addr, int value) {
        // TODO care about synchronization when writting
    }

}
