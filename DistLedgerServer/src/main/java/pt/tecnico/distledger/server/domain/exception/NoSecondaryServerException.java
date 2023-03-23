package pt.tecnico.distledger.server.domain.exception;

public class NoSecondaryServerException extends Exception {
    public NoSecondaryServerException() {
        super("No secondary server available");
    }
}