package pt.tecnico.distledger.server;

import com.google.longrunning.ListOperationsResponse;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.server.domain.operation.*;
import java.util.List;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

import static io.grpc.Status.INVALID_ARGUMENT;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ServerState state;

    public AdminServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        try {
            state.activate();
            ActivateResponse response = ActivateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (e.getMessage().equals("ALREADY_ACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Server was already active").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        try {
            state.deactivate();
            DeactivateResponse response = DeactivateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (e.getMessage().equals("ALREADY_INACTIVE")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Server was already inactive").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void getLedgerState(GetLedgerStateRequest request, StreamObserver<GetLedgerStateResponse> responseObserver) {
        List<Operation> ledger = state.getLedger();

        LedgerState.Builder ledgerStateBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();
        for (Operation op : ledger) {
            DistLedgerCommonDefinitions.Operation operation;
            if (op instanceof CreateOp) {
                operation = DistLedgerCommonDefinitions.Operation.newBuilder().setType(OperationType.OP_CREATE_ACCOUNT).setUserId(op.getAccount()).build();
            }
            else if (op instanceof DeleteOp) {
                operation = DistLedgerCommonDefinitions.Operation.newBuilder().setType(OperationType.OP_DELETE_ACCOUNT).setUserId(op.getAccount()).build();
            }
            else if (op instanceof TransferOp) {
                operation = DistLedgerCommonDefinitions.Operation.newBuilder().setType(OperationType.OP_TRANSFER_TO).setUserId(op.getAccount()).setDestUserId(((TransferOp) op).getDestAccount()).setAmount(((TransferOp) op).getAmount()).build();
            }
            else {
                operation = DistLedgerCommonDefinitions.Operation.newBuilder().setType(OperationType.OP_UNSPECIFIED).setUserId(op.getAccount()).build();
            }
            ledgerStateBuilder.addLedger(operation);
        }
        LedgerState ledgerState = ledgerStateBuilder.build();

        GetLedgerStateResponse response = GetLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }
}
