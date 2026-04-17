package ids.alet_zlatevski.controller;

import ids.alet_zlatevski.ControllerAccess;
import ids.alet_zlatevski.controller.rabbitmq.MutualExclusionHandler;
import ids.alet_zlatevski.controller.rmi.RMIHandler;
import ids.alet_zlatevski.memory.Memory;


import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.MEMORY_SIZE;

public class Controller implements ControllerAccess {

    // Config values
    protected final String nodeId;
    protected final String brokerHostname;

    // Worker objects
    protected Memory memory;
    protected RMIHandler rmiHandler;
    protected MutualExclusionHandler mutualExclusionHandler;

    public Controller(String brokerHostname, String nodeId) {
        this.brokerHostname = brokerHostname;
        this.nodeId = nodeId;
        setupMemory();
        setupNode();
        setupRegistry();
    }

    private void setupNode() {
        try {
            this.mutualExclusionHandler = new MutualExclusionHandler(this.brokerHostname, nodeId, this);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.err.println("IO Exception at " + this.brokerHostname);
            System.err.println("Exiting!");
            System.exit(130);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupRegistry() {
        try {
            this.rmiHandler = new RMIHandler(this);
            System.out.println("RMI registry started on port: " + this.rmiHandler.getPort());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception during Registry setup!");
            System.err.println("Exiting!");
            System.exit(130);
        }
    }

    private void setupMemory() {
        this.memory = new Memory(MEMORY_SIZE);
    }

    public void updateBroadcastChanges(int addr, long value) throws InterruptedException {
        this.memory.write(addr, value);
    }

    @Override
    public void write(int addr, long value) throws InterruptedException, IOException {
        this.mutualExclusionHandler.initiateWantLock();
        this.memory.write(addr, value);
        this.mutualExclusionHandler.initiateGlobalUpdateProcedure(addr, value);
        this.mutualExclusionHandler.initiateUnlock();
    }

    @Override
    public long read(int addr) throws InterruptedException {
        return this.memory.read(addr);
    }

    @Override
    public long increment(int addr) throws InterruptedException, IOException {
        return this.add(addr, 1);
    }

    @Override
    public long add(int addr, long delta) throws InterruptedException, IOException {
        this.mutualExclusionHandler.initiateWantLock();
        long value = this.memory.read(addr) + delta;
        this.memory.write(addr, value);
        this.mutualExclusionHandler.initiateGlobalUpdateProcedure(addr, value);
        this.mutualExclusionHandler.initiateUnlock();
        return value;
    }

    @Override
    public int size() throws RemoteException {
        return MEMORY_SIZE;
    }
}
