package pt.tecnico.distledger.server.domain.exception;

public class NonExistingAccountException extends IllegalArgumentException {

    public NonExistingAccountException(String account) {
        super("Account " + account + " does not exist");
    }
}
