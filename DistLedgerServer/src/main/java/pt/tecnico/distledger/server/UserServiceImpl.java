package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.vectorclock.VectorClock;
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
        List<Integer> prevTS = request.getPrevTSList();
        try {
            VectorClock newTS = state.createAccount(username, new VectorClock(prevTS));
            CreateAccountResponse response = CreateAccountResponse.newBuilder()
                    .addAllTS(newTS.toList())
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
        List<Integer> prevTS = request.getPrevTSList();
        try {
            int balance = state.getBalance(username, new VectorClock(prevTS));
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance)
                    .addAllValueTS(state.getValueTimestamp())
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
        List<Integer> prevTS = request.getPrevTSList();
        try {
            VectorClock newTS = state.transferTo(from, to, amount, new VectorClock(prevTS));
            TransferToResponse response = TransferToResponse.newBuilder()
                    .addAllTS(newTS.toList())
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
