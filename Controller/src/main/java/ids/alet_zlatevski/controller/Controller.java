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
            System.out.println("IO Exception at " + this.brokerHostname);
            System.out.println("Exiting!");
            System.exit(130);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupRegistry() {
        try {
            this.rmiHandler = new RMIHandler(this);
        } catch (Exception e) {
            System.err.println("err" + e);
            e.printStackTrace();
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
    public int size() throws RemoteException {
        return MEMORY_SIZE;
    }
}
