package ids.alet_zlatevski;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ControllerAccess extends Remote {
    void write(long addr, int value) throws RemoteException;
    int read(long addr) throws RemoteException;
    int size() throws RemoteException;
}