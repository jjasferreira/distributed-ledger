package pt.tecnico.distledger.server.domain.exception;

public class InvalidAmountException extends IllegalArgumentException {

    public InvalidAmountException(String amount) {
        super("Amount must be a positive integer, " + amount + "is not valid");
    }
}
