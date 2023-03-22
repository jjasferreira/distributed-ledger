package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.stub.StreamObserver;


public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private final ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        LedgerState ledgerState = request.getState();
        for (DistLedgerCommonDefinitions.Operation op : ledgerState.getLedgerList()) {

            if (op.getType() == OperationType.OP_CREATE_ACCOUNT) {
                state.createAccount(op.getUserId());
            }
            else if (op.getType() == OperationType.OP_TRANSFER_TO) {
                state.transferTo(op.getUserId(), op.getDestUserId(), op.getAmount());
            }
            else if (op.getType() == OperationType.OP_DELETE_ACCOUNT) {
                state.deleteAccount(op.getUserId());
            }
            else if (op.getType() == OperationType.OP_UNSPECIFIED) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Operation type not specified").asRuntimeException());
            }
        }
        PropagateStateResponse response = PropagateStateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}