package ids.alet_zlatevski;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ControllerAccess extends Remote {
    void write(int addr, long value) throws IOException, InterruptedException;
    long read(int addr) throws RemoteException, InterruptedException;
    int size() throws RemoteException;
}