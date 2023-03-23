package pt.tecnico.distledger.server.domain.exception;

public class UnknownOperationException extends IllegalArgumentException {

    public UnknownOperationException(String op) {
        super("Unknown operation: " + op);
    }
}
