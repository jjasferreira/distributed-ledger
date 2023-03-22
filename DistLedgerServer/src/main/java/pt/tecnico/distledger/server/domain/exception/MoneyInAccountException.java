package pt.tecnico.distledger.server.domain.exception;

public class MoneyInAccountException extends IllegalArgumentException {

    public MoneyInAccountException(String account) {
        super("There is money in the account " + account + ", so it cannot be deleted");
    }
}
