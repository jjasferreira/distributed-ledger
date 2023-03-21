package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;

public class NamingServerService {

    final ManagedChannel channel;

    NamingServerServiceGrpc.NamingServerServiceBlockingStub stub;

    public NamingServerService(String host, String port) {
        final String target = host + ":" + port;
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = NamingServerServiceGrpc.newBlockingStub(channel);
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

    public void shutdownNow() {
		channel.shutdownNow();
    }
}
