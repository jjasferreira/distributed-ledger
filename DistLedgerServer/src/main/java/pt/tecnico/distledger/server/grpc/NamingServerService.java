package pt.tecnico.distledger.server.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class NamingServerService {

    final ManagedChannel channel;

    NamingServerServiceGrpc.NamingServerServiceBlockingStub stub;

    public NamingServerService(String host, int port) {
        final String target = host + ":" + port;
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = NamingServerServiceGrpc.newBlockingStub(channel);
    }

    public String register(String name, String role, String address) {
        RegisterRequest request = RegisterRequest.newBuilder().setServiceName(name).setRole(role).setAddress(address).build();
        RegisterResponse response = stub.register(request);
        return response.toString();
    }

    public HashMap<String, String> lookup(String name, String role) {
        LookupRequest request = LookupRequest.newBuilder().setServiceName(name).setRole(role).build();
        LookupResponse response = stub.lookup(request);
        HashMap<String, String> servers = new HashMap<>();
        for (Server server : response.getServersList()) {
            servers.put(server.getAddress(), server.getRole());
        }
        return servers;
    }
    
    public String delete(String name, String address) {
        DeleteRequest request = DeleteRequest.newBuilder().setServiceName(name).setAddress(address).build();
        DeleteResponse response = stub.delete(request);
        return response.toString();
    }

    public void shutdownNow() {
		channel.shutdownNow();
    }
}
