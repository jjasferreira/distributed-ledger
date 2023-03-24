package pt.tecnico.distledger.namingserver.domain.exception;

public class AlreadyRegisteredAddressException extends IllegalArgumentException {

    public AlreadyRegisteredAddressException(String address, String name) {
        super("Address " + address + " is already registered to " + name);
    }
}
