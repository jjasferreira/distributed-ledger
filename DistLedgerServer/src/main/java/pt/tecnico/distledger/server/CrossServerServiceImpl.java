package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class CrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private final ServerState state;

    public CrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {

        List<Operation> ledger = new ArrayList<>();

        LedgerState state = request.getState();

        for (DistLedgerCommonDefinitions.Operation op : state.getLedgerList()) {

            if (op.getType() == OperationType.OP_CREATE_ACCOUNT) {
                ledger.add(new CreateOp(op.getUserId()));
            }
            else if (op.getType() == OperationType.OP_TRANSFER_TO) {
                ledger.add(new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount()));
            }
            else if (op.getType() == OperationType.OP_DELETE_ACCOUNT) {
                ledger.add(new DeleteOp(op.getUserId()));
            }
            else if (op.getType() == OperationType.OP_UNSPECIFIED) {
                ledger.add(new Operation(op.getUserId()));
            }
        }

        PropagateStateResponse response = PropagateStateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}