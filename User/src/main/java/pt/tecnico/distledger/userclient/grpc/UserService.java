package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.user.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

public class UserService {

    /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    final ManagedChannel channel;

    UserServiceGrpc.UserServiceBlockingStub stub;

    public UserService(String host, int port) {
        final String target = host + ":" + port;

        // Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

        // It is up to the client to determine whether to block the call.
		// Here we create a blocking stub, but an async stub,
		// or an async stub with Future are always possible.
        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public void createAccount(String server, String username) {
        try{
            CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build();
            CreateAccountResponse response = stub.createAccount(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void deleteAccount(String server, String username) {
        try{
            DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();
            DeleteAccountResponse response = stub.deleteAccount(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void balance(String server, String username) {
        try{
            BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
            BalanceResponse response = stub.balance(request);
            System.out.println("OK");
            System.out.println(response);
        }
        catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
        }
    }

    public void tranferTo(String server, String from, String dest, Integer amount) {
        try{
            TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();
            TransferToResponse response = stub.transferTo(request);
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
