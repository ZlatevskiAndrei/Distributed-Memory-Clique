package ids.alet_zlatevski.controller.rmi;

import ids.alet_zlatevski.ControllerAccess;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIHandler {
    protected ControllerAccess controllerAccess_stub;
    private final int port;

    public RMIHandler(ControllerAccess owner) throws RemoteException {
        // Find an available port
        try (ServerSocket socket = new ServerSocket(0)) {
            this.port = socket.getLocalPort();
        } catch (IOException e) {
            throw new RemoteException("Could not find an available port for RMI Registry", e);
        }

        this.controllerAccess_stub = (ControllerAccess) UnicastRemoteObject.exportObject(owner, 0);
        Registry registry = LocateRegistry.createRegistry(this.port);
        registry.rebind("API", this.controllerAccess_stub);
    }

    public int getPort() {
        return port;
    }
}
