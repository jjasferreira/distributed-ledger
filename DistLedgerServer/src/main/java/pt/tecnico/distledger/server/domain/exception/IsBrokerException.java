package pt.tecnico.distledger.server.domain.exception;

public class IsBrokerException extends IllegalArgumentException {

    public IsBrokerException() {
        super("Cannot do that operation on a broker");
    }
}
