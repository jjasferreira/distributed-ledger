package pt.tecnico.distledger.server.domain.exception;

public class AlreadyInactiveServerException extends IllegalArgumentException {

    public AlreadyInactiveServerException() {
        super("Server is already inactive");
    }

}
