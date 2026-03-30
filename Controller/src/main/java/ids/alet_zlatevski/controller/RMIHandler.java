package ids.alet_zlatevski.controller;

import ids.alet_zlatevski.ControllerAccess;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIHandler {
    protected ControllerAccess controllerAccess_stub;

    public RMIHandler(ControllerAccess controllerAccess) throws RemoteException {
        this.controllerAccess_stub = (ControllerAccess) UnicastRemoteObject.exportObject(controllerAccess, 0);
        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("API", this.controllerAccess_stub);
    }

}
