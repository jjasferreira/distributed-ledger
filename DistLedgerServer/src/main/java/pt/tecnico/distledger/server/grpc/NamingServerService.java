package pt.tecnico.distledger.server.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import pt.ulisboa.tecnico.distledger.contract.naming.ServerServiceGrpc;

public class NamingServerService {

    final ManagedChannel channel;

    ServerServiceGrpc.ServerServiceBlockingStub stub;

    public NamingServerService(String host, int port) {
        final String target = host + ":" + port;
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = ServerServiceGrpc.newBlockingStub(channel);
    }

    public void shutdownNow() {
        channel.shutdownNow();
    }

    public String lookup() {
        LookupRequest request = LookupRequest.getDefaultInstance();
        LookupResponse response = stub.lookup(request);
        return response.toString();
    }

    public String register(String server) {
        RegisterRequest request = RegisterRequest.newBuilder().setServer(server).build();
        RegisterResponse response = stub.register(request);
        return response.toString();
    }

}
