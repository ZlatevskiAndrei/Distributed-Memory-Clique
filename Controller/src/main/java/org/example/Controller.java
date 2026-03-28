package org.example;

import com.rabbitmq.client.*;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeoutException;

public class Controller implements ControllerAccess{
    public static int MEMORY_SIZE = 0x400;
    private int[] memory;
    private final String brokerHostname;


    public Controller(String brokerHostname) {
        this.brokerHostname = brokerHostname;
        setupMemory();
        setupNode();
        setupRegistry();
    }

    public void setupNode() {
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
        }
        catch (IOException e) {
            System.out.println("IO Exception at " + this.brokerHostname);
            System.out.println("Exiting!");
            System.exit(130);
        } catch (TimeoutException t) {
            t.printStackTrace();
        }
    }

    public void setupRegistry() {
        try {
            ControllerAccess controllerAccess_stub = (ControllerAccess) UnicastRemoteObject.exportObject(this,0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("API", controllerAccess_stub);
        }
        catch (Exception e ) {
            System.err.println("err" + e);
            e.printStackTrace();
        }
    }

    public void setupMemory() {
        this.memory = new int[MEMORY_SIZE];
    }

    @Override
    public void write(long addr, int value) throws RemoteException {

    }

    @Override
    public int read(long addr) throws RemoteException {
        return 0;
    }

}
