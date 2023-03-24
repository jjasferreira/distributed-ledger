package pt.tecnico.distledger.namingserver.domain.exception;

public class AlreadyRegisteredRoleException extends IllegalArgumentException {

    public AlreadyRegisteredRoleException(String role, String name) {
        super("Role " + role + " is already registered to " + name);
    }
}
