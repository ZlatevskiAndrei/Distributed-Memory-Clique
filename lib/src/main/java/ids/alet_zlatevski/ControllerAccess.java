package ids.alet_zlatevski;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ControllerAccess extends Remote {
    void write(int addr, long value) throws IOException, InterruptedException;
    long read(int addr) throws RemoteException, InterruptedException;
    long increment(int addr) throws InterruptedException, IOException;
    long add(int addr, long delta) throws InterruptedException, IOException;
    int size() throws RemoteException;
}