package pt.tecnico.distledger.server;


import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import io.grpc.stub.StreamObserver;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    /*TODO: The gRPC server-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

    private final ServerState state;

    public UserServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUserId();
        int balance = state.getBalance(username);
        BalanceResponse response = BalanceResponse.newBuilder().setValue(balance).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        String username = request.getUserId();
        state.createAccount(username);
        CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        String username = request.getUserId();
        state.deleteAccount(username);
        DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        String from = request.getAccountFrom();
        String to = request.getAccountTo();
        int amount = request.getAmount();
        state.transferTo(from, to, amount);
        TransferToResponse response = TransferToResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
