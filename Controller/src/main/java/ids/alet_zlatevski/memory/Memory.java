package ids.alet_zlatevski.memory;


import java.util.*;

public class Memory {

    protected final int size;
    protected final long[] memory;
    /**
     * TODO we have here the concrete example of that we are not consistent in our vision of
     *  what we lock. As here the memory has a "by address granularity" where our mutualExclusionHandler has a
     *  global memory block this.mutualExclusionHandler.initiateWantLock() we lock the whole memory.
     *  For now at maximum the addressStatuses can contain only one address with the BUSY status, so
     *  the haspmap is "useless".
     * <p>
     * We have 2 choices :
     * - set the granularity of the memory to only one write at time (removing the hashap, replacing
     * it by an address, addressStatus. (easy and simple)
     * - set the granularity of the mutualExclusionHandler to "by address".
     * (need more changes, may be a bit tricky to test.
     * <p>
     * <p>
     * My proposition first go with the simple granularity of the memory, if we finish early we may introduce
     * the granularity for the mutualExclusionHandler. For now don't touch to anything this is not
     * a blocking issue we will discuss about it.
     */
    protected final Set<Integer> busyAddr;

    public Memory(final int size) {
        this.size = size;
        this.memory = new long[size];
        this.busyAddr = new HashSet<>();
    }

    protected void setAddressStatus(int addr, AddressStatus status) {
        if (status == AddressStatus.FREE) {
            assert (this.busyAddr.contains(addr));
            this.busyAddr.remove(addr);
        } else if (status == AddressStatus.BUSY) {
            assert (!this.busyAddr.contains(addr));
            this.busyAddr.add(addr);
        } else {
            assert (false);
        }
    }

    protected boolean isAddressBusy(int addr) {
        return this.busyAddr.contains(addr);
    }

    protected void setValue(int addr, long value) {
        this.memory[addr] = value;
    }

    public void write(int addr, long value) throws InterruptedException {
        synchronized (this) {
            // if the currently addr is being written we wait.
            // Should not be possible as the critical section is also managed by mutualExclusionHandler.
            while (this.isAddressBusy(addr)) {
                this.wait();
            }

            this.setAddressStatus(addr, AddressStatus.BUSY);
            this.setValue(addr, value);

            Thread.sleep(400); // for simulation purposes, if multiple local clients want to access this addr

            this.setAddressStatus(addr, AddressStatus.FREE);
            this.notifyAll(); // notify threads waiting on memory
        }
    }

    public long read(int addr) throws InterruptedException {
        synchronized (this) {
            while (this.isAddressBusy(addr)) {
                this.wait();
            }
            return this.memory[addr];
        }
    }

}
