package pt.tecnico.distledger.server.domain.exception;

public class AlreadyExistingAccountException extends IllegalArgumentException {

    public AlreadyExistingAccountException(String account) {
        super("Account " + account + " already exists");
    }

}
