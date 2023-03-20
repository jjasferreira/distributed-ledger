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

    public String register(String serviceName, String role, String address) {
        RegisterRequest request = RegisterRequest.newBuilder().setServiceName(serviceName).setRole(role).setAddress(address).build();
        RegisterResponse response = stub.register(request);
        return response.toString();
    }

    public String lookup(String serviceName, String role) {
        LookupRequest request = LookupRequest.newBuilder().setServiceName(serviceName).setRole(role).build();
        LookupResponse response = stub.lookup(request);
        return response.toString();
    }
    
    public String delete(String serviceName, String address) {
        DeleteRequest request = DeleteRequest.newBuilder().setServiceName(serviceName).setAddress(address).build();
        DeleteResponse response = stub.delete(request);
        return response.toString();
    }

    public void shutdownNow() {
		channel.shutdownNow();
    }
}
