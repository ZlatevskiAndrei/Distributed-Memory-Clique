package ids.alet_zlatevski.controller;

import ids.alet_zlatevski.ControllerAccess;
import ids.alet_zlatevski.operations.Operation;
import ids.alet_zlatevski.operations.OperationStatus;
import ids.alet_zlatevski.operations.ReadOperation;
import ids.alet_zlatevski.operations.WriteOperation;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Controller implements ControllerAccess {

    public static final int MEMORY_SIZE = 0x400;
    protected int[] memory;
    protected final String brokerHostname;
    protected final BlockingQueue<Operation> pendingOperations;

    protected MutualExclusionHandler mutualExclusionHandler;


    public Controller(String brokerHostname) {
        this.brokerHostname = brokerHostname;
        this.pendingOperations = new LinkedBlockingDeque<>();
        setupMemory();
        setupNode();
        setupRegistry();
    }

    private void setupNode() {
        this.mutualExclusionHandler = new MutualExclusionHandler(this.brokerHostname);
    }

    private void setupRegistry() {
        try {
            ControllerAccess controllerAccess_stub = (ControllerAccess) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("API", controllerAccess_stub);
        } catch (Exception e) {
            System.err.println("err" + e);
            e.printStackTrace();
        }
    }

    private void setupMemory() {
        this.memory = new int[MEMORY_SIZE];
    }

    protected synchronized void queueOperation(final Operation operation) {
        this.pendingOperations.add(operation);
        operation.setStatus(OperationStatus.PENDING);
        try {
            Thread.currentThread().wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void write(long addr, int value) throws RemoteException {
        Operation new_op = new WriteOperation(addr, value);
        this.queueOperation(new_op);
        assert (new_op.getStatus() == OperationStatus.DONE);
//        this.mutualExclusionHandler. TODO may use the mutualExclusionHandler instead of the blocking queue
    }

    @Override
    public synchronized int read(long addr) throws RemoteException {
        // TODO think about the read operation ? Do we need to pass though the controller main loop logic ? As the
        //  read operation isn't blocked by any action ?
        ReadOperation new_op = new ReadOperation(addr);
        this.queueOperation(new_op);
        assert (new_op.getStatus() == OperationStatus.DONE && new_op.getResult() != null);
        return new_op.getResult();
    }

    @Override
    public int size() throws RemoteException {
        return MEMORY_SIZE;
    }

}
