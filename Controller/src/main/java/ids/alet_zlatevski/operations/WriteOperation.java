package ids.alet_zlatevski.operations;

public class WriteOperation extends Operation {
    protected final long addr;
    protected final int value;

    public WriteOperation(long addr, int value) {
        super(OperationType.WRITE);
        this.addr = addr;
        this.value = value;
    }

    public long getAddr() {
        return addr;
    }

    public int getValue() {
        return value;
    }
}
