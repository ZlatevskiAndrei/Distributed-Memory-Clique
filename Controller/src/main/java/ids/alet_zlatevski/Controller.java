package ids.alet_zlatevski;

import com.rabbitmq.client.*;
import com.rabbitmq.client.ConnectionFactory;
import ids.alet_zlatevski.operations.Operation;
import ids.alet_zlatevski.operations.OperationStatus;
import ids.alet_zlatevski.operations.ReadOperation;
import ids.alet_zlatevski.operations.WriteOperation;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;

public class Controller implements ControllerAccess, Runnable {

    public static final int MEMORY_SIZE = 0x400;
    protected int[] memory;
    protected final String brokerHostname;
    protected final BlockingQueue<Operation> pendingOperations;

    protected final Thread internalThread;


    public Controller(String brokerHostname) {
        this.brokerHostname = brokerHostname;
        this.pendingOperations = new LinkedBlockingDeque<>();
        this.internalThread = new Thread(this);
        setupMemory();
        setupNode();
        setupRegistry();
        startInternalLoop();
    }

    private void startInternalLoop() {
        this.internalThread.start();
    }

    @Override
    public void run() {
        while (true) {
            // TODO internal loop read over rabbitMQ broadcast, and notify pendingOperation when done.
// Operation operation = this.pendingOperations.poll(10, TimeUnit.MILLISECONDS);
        }
    }

    private void setupNode() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(brokerHostname);
        Channel channel;
        Connection connection;
        long pid = ProcessHandle.current().pid();
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.queueDeclare("BROADCAST", false, true, true, null);
            channel.queueDeclare(String.valueOf(pid), false, true, true, null);
        } catch (IOException e) {
            System.out.println("IO Exception at " + this.brokerHostname);
            System.out.println("Exiting!");
            System.exit(130);
        } catch (TimeoutException t) {
            t.printStackTrace();
        }
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
