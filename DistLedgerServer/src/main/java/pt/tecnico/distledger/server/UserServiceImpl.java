package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;

import java.util.List;


public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState state;

    public UserServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        String username = request.getUserId();
        List<Integer> prevTS = request.getPrevTS();
        try {
            List<Integer> newTS = state.createAccount(username, prevTS);
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                    .setTS(newTS)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InactiveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (AlreadyExistingAccountException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUserId();
        List<Integer> prevTS = request.getPrevTS();
        try {
            int balance = state.getBalance(username, prevTS);
            List<Integer> newTS = state.getValueTS();
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance)
                    .setValueTS(newTS)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InactiveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (NonExistingAccountException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        String from = request.getAccountFrom();
        String to = request.getAccountTo();
        int amount = request.getAmount();
        List<Integer> prevTS = request.getPrevTS();
        try {
            List<Integer> newTS = state.transferTo(from, to, amount, prevTS);
            TransferToResponse response = TransferToResponse.newBuilder()
                    .setTS(newTS)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InactiveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (NonExistingAccountException | NotEnoughMoneyException | InvalidAmountException | SameAccountException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

}
