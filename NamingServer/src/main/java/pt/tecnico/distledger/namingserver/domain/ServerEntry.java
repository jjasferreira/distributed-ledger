package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {

    private String role;

    private String address;

    public ServerEntry(String role, String address) {
        this.role = role;
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ServerEntry{" +
                "role=" + role +
                ", address=" + address +
                '}';
    }

}
