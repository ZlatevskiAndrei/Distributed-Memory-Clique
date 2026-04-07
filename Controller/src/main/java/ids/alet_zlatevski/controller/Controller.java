package ids.alet_zlatevski.controller;

import ids.alet_zlatevski.ControllerAccess;
import ids.alet_zlatevski.controller.rabbitmq.MutualExclusionHandler;
import ids.alet_zlatevski.memory.Memory;


import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.MEMORY_SIZE;
import static ids.alet_zlatevski.env.Constants.NODE_NUMBER;

public class Controller implements ControllerAccess {

    protected Memory memory;
    protected RMIHandler rmiHandler;
    protected MutualExclusionHandler mutualExclusionHandler;
    protected final String nodeId;
    protected final String brokerHostname;
    protected final Semaphore handshakeSemaphore;
    protected final Semaphore lockSemaphore;


    public Controller(String brokerHostname, String nodeId) {
        this.brokerHostname = brokerHostname;
        this.nodeId = nodeId;
        this.handshakeSemaphore = new Semaphore(0);
        this.lockSemaphore = new Semaphore(0);
        setupMemory();
        setupNode(); //semaphore ensures that all nodes are handshaking before the client could use the api.
        setupRegistry();
    }

    private void setupNode() {
        try {
            this.mutualExclusionHandler = new MutualExclusionHandler(this.brokerHostname, NODE_NUMBER, nodeId, handshakeSemaphore, lockSemaphore);
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
        this.memory = new Memory(MEMORY_SIZE, mutualExclusionHandler);
    }


    @Override
    public void write(long addr, int value) throws IOException, InterruptedException {
        this.mutualExclusionHandler.initiateWantLock();
        //now we have the lock and the semaphore is released, we modify the memory values and the status of the addresses, as we may have local clients trying to read
        this.mutualExclusionHandler.initiateUnlock(); //we let the other nodes grab the lock
    }

    @Override
    public int read(long addr) throws RemoteException {
        // TODO think about the read operation ? Do we need to pass though the controller main loop logic ? As the
        //  read operation isn't blocked by any action ?
        return 0;
    }

    @Override
    public int size() throws RemoteException {
        return MEMORY_SIZE;
    }

}
