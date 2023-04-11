package pt.tecnico.distledger.server.domain.exception;

public class AlreadyActiveServerException extends IllegalArgumentException {

    public AlreadyActiveServerException() {
        super("Server is already active");
    }

}
