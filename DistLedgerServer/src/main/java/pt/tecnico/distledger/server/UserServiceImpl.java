package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;


public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState state;

    public UserServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        String username = request.getUserId();
        try {
            int balance = state.getBalance(username);
            BalanceResponse response = BalanceResponse.newBuilder().setValue(balance).build();
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
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        String username = request.getUserId();
        try {
            state.createAccount(username);
            CreateAccountResponse response = CreateAccountResponse.newBuilder().build();
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
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        String username = request.getUserId();
        try {
            if (!state.isPrimaryServer()) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Cannot perform write operation on a secondary server").asRuntimeException());
            }
            state.deleteAccount(username);
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InactiveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (NonExistingAccountException | MoneyInAccountException | IsBrokerException e) {
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
        try {
            state.transferTo(from, to, amount);
            TransferToResponse response = TransferToResponse.newBuilder().build();
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
