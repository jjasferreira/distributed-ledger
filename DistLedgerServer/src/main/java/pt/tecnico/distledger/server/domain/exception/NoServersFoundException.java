package pt.tecnico.distledger.server.domain.exception;

public class NoServersFoundException extends IllegalArgumentException {

    public NoServersFoundException() {
        super("No servers found to gossip with");
    }

}