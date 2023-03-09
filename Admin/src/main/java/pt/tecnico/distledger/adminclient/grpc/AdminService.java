package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.admin.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

public class AdminService {

    /* TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

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

    public void activate(String server) {
        try{
            ActivateRequest request = ActivateRequest.getDefaultInstance();
            ActivateResponse response = stub.activate(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void deactivate(String server) {
        try{
            DeactivateRequest request  = DeactivateRequest.getDefaultInstance();
            DeactivateResponse response = stub.deactivate(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void getLedgerState(String server) {
        try{
            getLedgerStateRequest request = getLedgerStateRequest.getDefaultInstance();
            getLedgerStateResponse response = stub.getLedgerState(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void shutdownNow() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }
}
