package pt.tecnico.distledger.namingserver.domain.exception;

public class NonExistingAddressException extends IllegalArgumentException {

    public NonExistingAddressException(String address, String name) {
        super("Address " + address + " does not exist in " + name);
    }
}