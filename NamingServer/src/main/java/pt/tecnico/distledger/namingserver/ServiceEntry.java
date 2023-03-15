package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.ServerEntry;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {

    private String name;

    private List<ServerEntry> servers = new ArrayList<ServerEntry>();

    public ServiceEntry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<ServerEntry> getServers() {
            return servers;
    }

    public void addServer(ServerEntry server){
        servers.add(server);
    }

    public void removeServer(String address){
        servers.removeIf(s -> s.getAddress().equals(address));
    }

}