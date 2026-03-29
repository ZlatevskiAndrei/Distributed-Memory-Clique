package ids.alet_zlatevski.operations;

public class ReadOperation extends Operation {
    protected final long addr;
    protected Integer result = null;

    public ReadOperation(long addr) {
        super(OperationType.READ);
        this.addr = addr;
    }

    public long getAddr() {
        return addr;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Integer getResult() {
        return result;
    }
}
