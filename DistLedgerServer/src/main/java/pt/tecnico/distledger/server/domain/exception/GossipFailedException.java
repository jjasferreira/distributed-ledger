package pt.tecnico.distledger.server.domain.exception;

public class GossipFailedException extends IllegalArgumentException {

    public GossipFailedException(String role) {
        super("Gossiping with server " + role + " failed");
    }

}
