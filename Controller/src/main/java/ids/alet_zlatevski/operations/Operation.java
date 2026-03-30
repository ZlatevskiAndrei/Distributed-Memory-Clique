package ids.alet_zlatevski.operations;

public abstract class Operation {

    protected final OperationType type;
    protected OperationStatus status;
    protected final Thread operationOwner;

    public Operation(final OperationType type) {
        this.type = type;
        this.status = OperationStatus.CREATED;
        this.operationOwner = Thread.currentThread();
    }

    public OperationType getType() {
        return type;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(final OperationStatus status) {
        this.status = status;
    }

    public void resumeOwner() {
        this.operationOwner.notify();
    }
}
