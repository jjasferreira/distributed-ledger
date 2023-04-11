package pt.tecnico.distledger.namingserver.domain.exception;

public class TooManyServersException extends IllegalArgumentException {

    public TooManyServersException(String role) {
        super("Too many servers with role " + role);
    }
}