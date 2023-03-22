package pt.tecnico.distledger.server.domain.exception;

public class SameAccountException extends IllegalArgumentException {

    public SameAccountException(String account) {
        super("Sender and receiver are the same account: " + account);
    }
}
