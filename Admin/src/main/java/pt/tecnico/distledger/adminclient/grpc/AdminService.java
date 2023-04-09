package pt.tecnico.distledger.adminclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AdminService {

    final ManagedChannel channel;

    AdminServiceGrpc.AdminServiceBlockingStub stub;

    public AdminService(String host, int port) {
        final String target = host + ":" + port;

        // Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // It is up to the client to determine whether to block the call.
		// Here we create a blocking stub, but an async stub,
		// or an async stub with Future are always possible.
		stub = AdminServiceGrpc.newBlockingStub(channel);
    }

    public String activate() {
        ActivateRequest request = ActivateRequest.getDefaultInstance();
        ActivateResponse response = stub.activate(request);
        return response.toString();
    }

    public String deactivate() {
        DeactivateRequest request  = DeactivateRequest.getDefaultInstance();
        DeactivateResponse response = stub.deactivate(request);
        return response.toString();
    }

    public String getLedgerState() {
        GetLedgerStateRequest request = GetLedgerStateRequest.getDefaultInstance();
        GetLedgerStateResponse response = stub.getLedgerState(request);
        return response.toString();
    }

    public void shutdownNow() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }

    public String gossip(String roleTo) {
        GossipRequest request = GossipRequest.newBuilder().setRole(roleTo).build();
        GossipResponse response = stub.gossip(request);
        return response.toString();
    }
}
