package org.example;

import java.rmi.RemoteException;

public class Test implements ControllerAccess {
    @Override
    public void write(long addr, int value) throws RemoteException {

    }

    @Override
    public int read(long addr) throws RemoteException {
        return 0;
    }
}
