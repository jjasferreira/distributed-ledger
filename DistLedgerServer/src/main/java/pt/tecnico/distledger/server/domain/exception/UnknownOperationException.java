package pt.tecnico.distledger.server.domain.exception;

public class UnknownOperationException extends IllegalArgumentException {

    public UnknownOperationException() {
        super("Unknown operation: ");
    }
}
