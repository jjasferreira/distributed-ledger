package pt.tecnico.distledger.namingserver;

public class ServerEntry {

    private String role;

    private String address;

    public ServerEntry(String name, String role, String address) {
        this.role = role;
        this.address = address;
    }

    //TODO: Do we need setters here?

    public String getRole() {
        return role;
    }

    public String getAddress() {
        return address;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void getAddressHost(){
        return address.split(":")[0];
    }

    public void getAddressPort(){
        return address.split(":")[1];
    }

}
