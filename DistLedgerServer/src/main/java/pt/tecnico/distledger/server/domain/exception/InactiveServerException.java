package pt.tecnico.distledger.server.domain.exception;

public class InactiveServerException extends IllegalArgumentException {

    public InactiveServerException(String role) {
        super("Server " + role + " is unavailable because it is inactive");
    }
}
