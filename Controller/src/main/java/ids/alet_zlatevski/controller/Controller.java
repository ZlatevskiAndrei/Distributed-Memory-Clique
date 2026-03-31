package ids.alet_zlatevski.controller;

import ids.alet_zlatevski.ControllerAccess;
import ids.alet_zlatevski.controller.rabbitmq.MutualExclusionHandler;
import ids.alet_zlatevski.memory.Memory;
import ids.alet_zlatevski.operations.Operation;
import ids.alet_zlatevski.operations.OperationStatus;
import ids.alet_zlatevski.operations.ReadOperation;
import ids.alet_zlatevski.operations.WriteOperation;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;

import static ids.alet_zlatevski.env.Constants.MEMORY_SIZE;
import static ids.alet_zlatevski.env.Constants.NODE_NUMBER;

public class Controller implements ControllerAccess {

    protected Memory memory;
    protected RMIHandler rmiHandler;
    protected MutualExclusionHandler mutualExclusionHandler;
    protected final String nodeId;
    protected final String brokerHostname;
    protected final BlockingQueue<Operation> pendingOperations;


    public Controller(String brokerHostname, String nodeId) {
        this.brokerHostname = brokerHostname;
        this.pendingOperations = new LinkedBlockingDeque<>();
        this.nodeId = nodeId;
        setupMemory();
        setupNode();
        setupRegistry();
    }

    private void setupNode() {
        try {
            this.mutualExclusionHandler = new MutualExclusionHandler(this.brokerHostname, NODE_NUMBER, nodeId);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
            System.out.println("IO Exception at " + this.brokerHostname);
            System.out.println("Exiting!");
            System.exit(130);
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
    public void write(long addr, int value) throws RemoteException {
        Operation new_op = new WriteOperation(addr, value);
        this.queueOperation(new_op);
        assert (new_op.getStatus() == OperationStatus.DONE);
//        this.mutualExclusionHandler. TODO may use the mutualExclusionHandler instead of the blocking queue
    }

    @Override
    public int read(long addr) throws RemoteException {
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
