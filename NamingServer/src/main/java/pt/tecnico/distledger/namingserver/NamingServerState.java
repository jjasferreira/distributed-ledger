package pt.tecnico.distledger.namingserver;

import java.util.HashMap;

//TODO is this the right class name
public class NamingServerState {
    private HashMap<String, ServiceEntry> services = new HashMap<String, ServiceEntry>();

    public NamingServerState(){
    }

    public void addService(String name, String role, String address){
        services.put(name, new ServiceEntry(name, role, address));
    }

    public void removeService(String name){
        services.remove(name);
    }

    public ServiceEntry getService(String name){
        return services.get(name);
    }

    // TODO add server to service
}