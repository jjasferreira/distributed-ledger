package pt.tecnico.distledger.server.domain.exception;

public class WrongServerRoleException extends IllegalArgumentException {

    public WrongServerRoleException(String role) {
        super("Cannot do this on a server with role " + role);
    }
}
