package pt.tecnico.distledger.server.domain.exception;

public class InvalidTimestampException extends IllegalArgumentException {

    public InvalidTimestampException(String timestamp) {
        super("Client timestamp (" + timestamp + ") is more recent than server's");
    }

}
