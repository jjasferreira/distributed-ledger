package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.stub.StreamObserver;

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
        }
        catch (Exception e) {
            if (e.getMessage().equals("INACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("UNAVAILABLE").asRuntimeException());
            } else if (e.getMessage().equals("DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Account does not exist").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
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
        }
        catch (Exception e) {
            if (e.getMessage().equals("INACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("UNAVAILABLE").asRuntimeException());
            } else if (e.getMessage().equals("ALREADY_EXISTS")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Account already exists").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> responseObserver) {
        String username = request.getUserId();
        try {
            if (!state.isPrimary()) {
                reponseObserver.onError(INVALID_ARGUMENT.withDescription("Cannot perform write operation on a secondary server").asRuntimeException())
            }
            state.deleteAccount(username);
            DeleteAccountResponse response = DeleteAccountResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        //todo: catch blablaexception, catch blablablaexception, catch exception
        catch (Exception e) {
            if (e.getMessage().equals("INACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("UNAVAILABLE").asRuntimeException());
            } else if (e.getMessage().equals("DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Account does not exist").asRuntimeException());
            } else if (e.getMessage().equals("HAS_MONEY")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Account still has money").asRuntimeException());
            } else if (e.getMessage().equals("IS_BROKER")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Cannot delete broker account").asRuntimeException());
            } else {
                responseObserver.onError(UNKNOWN.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
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
        }
        catch (Exception e) {
            if (e.getMessage().equals("INACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("UNAVAILABLE").asRuntimeException());
            } else if (e.getMessage().equals("FROM_DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Sender account does not exist").asRuntimeException());
            } else if (e.getMessage().equals("TO_DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Receiver account does not exist").asRuntimeException());
            } else if (e.getMessage().equals("NOT_ENOUGH_MONEY")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Insufficient funds for transfer").asRuntimeException());
            } else if (e.getMessage().equals("INVALID_AMOUNT")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Amount must be greater than 0").asRuntimeException());
            } else if (e.getMessage().equals("SAME_ACCOUNT")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Sender and receiver are the same").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

}
