package pt.tecnico.distledger.namingserver.domain.exception;

public class NonExistingServiceException extends IllegalArgumentException {

    public NonExistingServiceException(String name) {
        super("Service " + name + " does not exist");
    }
}