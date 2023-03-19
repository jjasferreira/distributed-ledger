package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.exception.*;

import java.util.HashMap;

public class NamingServerState {

    private HashMap<String, ServiceEntry> services;

    private final boolean debug;

    public NamingServerState(boolean debug) {
        this.services = new HashMap<>();
        this.debug = debug;
    }

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }

    // register: Registers a new server in a service of the naming server list of services.
    public void register(String name, String role, String address) throws NamingServerStateException {
        debug("> Registering server " + address + " with role " + role " to the service " + name + "...");
        synchronized (services) {
            // TODO: Put as least code as possible inside the synchronized block
        }

        ServiceEntry service = services.get(name);
        // If the service exists, check if the role or address is already registered
        if (service != null) {
            for (ServerEntry server : service.getServers()) {
                if (server.getAddress().equals(address)) {
                    debug("NOK: address " + address + " already registered for service " + name);
                    throw new NamingServerStateException("ADDRESS_ALREADY_REGISTERED");
                }
                if (server.getRole().equals(role)) {
                    debug("NOK: role " + role + " already registered for service " + name);
                    throw new NamingServerStateException("ROLE_ALREADY_REGISTERED");
                }
            }
        } else { // If the service does not exist, create it
            service = new ServiceEntry(name);
            services.put(name, service);
        }
        ServerEntry server = new ServerEntry(role, address);
        service.addServer(server);
        debug("OK");
    }

    // delete: Deletes a server from a service of the naming server list of services.
    public void delete(String name, String address) throws NamingServerStateException {
        debug("> Deleting server " + address + " from the service " + name + "...");
        synchronized (services) {
            // TODO: Put as least code as possible inside the synchronized block
        }

        ServiceEntry service = services.get(name);
        if (service == null) {
            debug("NOK: service " + name + " does not exist");
            throw new NamingServerStateException("SERVICE_DOES_NOT_EXIST");
        }
        for (ServerEntry server : service.getServers()) {
            if (server.getAddress().equals(address)) {
                service.removeServer(address);
                debug("OK");
                return;
            }
        }
        debug("NOK: address " + address + " does not exist for service " + name);
        throw new NamingServerStateException("ADDRESS_DOES_NOT_EXIST");
    }
}