package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class UserService {

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

    public String createAccount(String username) {
        CreateAccountRequest request = CreateAccountRequest.newBuilder().setUserId(username).build();
        CreateAccountResponse response = stub.createAccount(request);
        return response.toString();
    }

    public String deleteAccount(String username) {
        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setUserId(username).build();
        DeleteAccountResponse response = stub.deleteAccount(request);
        return response.toString();
    }

    public String balance(String username) {
        BalanceRequest request = BalanceRequest.newBuilder().setUserId(username).build();
        BalanceResponse response = stub.balance(request);
        return response.toString();
    }

    public String transferTo(String from, String dest, Integer amount) {
        TransferToRequest request = TransferToRequest.newBuilder().setAccountFrom(from).setAccountTo(dest).setAmount(amount).build();
        TransferToResponse response = stub.transferTo(request);
        return response.toString();
    }

    public void shutdownNow() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }

}
