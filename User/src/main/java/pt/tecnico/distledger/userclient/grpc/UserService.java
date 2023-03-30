package pt.tecnico.distledger.userclient.grpc;

import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.tecnico.distledger.userclient.BalanceInfo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;

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

    public List<Integer> createAccount(String username, List<Integer> timestamp) {
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setUserId(username)
                .setPrevTS(timestamp)
                .build();
        CreateAccountResponse response = stub.createAccount(request);
        return response.getTSList();
    }

    public BalanceInfo balance(String username, List<Integer> timestamp) {
        BalanceRequest request = BalanceRequest.newBuilder()
                .setUserId(username)
                .setPrevTS(timestamp)
                .build();
        BalanceResponse response = stub.balance(request);
        Integer value = response.getValue();
        List<Integer> valueTS = response.getValueTSList();
        return new BalanceInfo(value, valueTS);
    }

    public List<Integer> transferTo(String from, String to, Integer amount, List<Integer> timestamp) {
        TransferToRequest request = TransferToRequest.newBuilder()
                .setAccountFrom(from)
                .setAccountTo(to)
                .setAmount(amount)
                .setPrevTS(timestamp)
                .build();
        TransferToResponse response = stub.transferTo(request);
        return response.getTSList();
    }

    public void shutdownNow() {
        // A Channel should be shutdown before stopping the process.
		channel.shutdownNow();
    }

}
