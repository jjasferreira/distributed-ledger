package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {

    private String name;

    private List<ServerEntry> servers;

    public ServiceEntry(String name) {
        this.name = name;
        this.servers = new ArrayList<>();
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