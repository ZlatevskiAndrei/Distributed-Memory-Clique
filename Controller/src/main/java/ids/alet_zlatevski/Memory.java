package ids.alet_zlatevski;

import ids.alet_zlatevski.controller.MutualExclusionHandler;
import ids.alet_zlatevski.operations.Operation;

import java.util.ArrayList;

public class Memory {

    protected final int size;
    protected final int[] memory;

    protected MutualExclusionHandler mutualExclusionHandler; // TODO may be usefull ?

    protected ArrayList<Operation> operations;

    public Memory(final int size, final MutualExclusionHandler mutualExclusionHandler) {
        this.size = size;
        this.memory = new int[size];
        this.mutualExclusionHandler = mutualExclusionHandler;
    }

    public void read(long addr) {
        // TODO care about synchronization when reading, becareful to check if the memory is lock or not in the mutualExclusionHandler.
    }

    public void write(long addr, int value) {
        // TODO care about synchronization when writting
    }

}
