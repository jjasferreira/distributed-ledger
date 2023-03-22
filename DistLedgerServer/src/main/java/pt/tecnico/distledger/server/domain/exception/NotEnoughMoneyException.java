package pt.tecnico.distledger.server.domain.exception;

public class NotEnoughMoneyException extends IllegalArgumentException {

    public NotEnoughMoneyException(String account) {
        super("Account " + account + " has insufficient funds");
    }
}
