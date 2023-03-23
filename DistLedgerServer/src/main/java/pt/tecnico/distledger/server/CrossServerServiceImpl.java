package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;


public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private final ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        LedgerState ledgerState = request.getState();
        try {
            for (DistLedgerCommonDefinitions.Operation op : ledgerState.getLedgerList()) {
                Operation operation;
                if (op.getType() == OperationType.OP_CREATE_ACCOUNT)
                    operation = new CreateOp(op.getUserId());
                else if (op.getType() == OperationType.OP_DELETE_ACCOUNT)
                    operation = new DeleteOp(op.getUserId());
                else if (op.getType() == OperationType.OP_TRANSFER_TO)
                    operation = new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount());
                else {
                    responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid operation type").asRuntimeException());
                    return;
                }
                state.receivePropagatedState(operation);
            }
            PropagateStateResponse response = PropagateStateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InactiveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (WrongServerRoleException | UnknownOperationException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

}