import java.rmi.Remote;

public interface ControllerAccess extends Remote {
    void write(long addr, int value);
    int read(long addr);
}