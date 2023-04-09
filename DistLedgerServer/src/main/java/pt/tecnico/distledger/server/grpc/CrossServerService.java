package pt.tecnico.distledger.server.grpc;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.server.domain.operation.*;

import io.grpc.StatusRuntimeException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.List;


public class CrossServerService {

    final ManagedChannel channel;

    DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub;

    public CrossServerService(String target) {
        // Receive target in the form of host:port
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
    }

    public boolean propagateState(List<Operation> ledger, List<Integer> replicaTS) {
        DistLedgerCommonDefinitions.LedgerState.Builder ledgerStateBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();
        for (Operation op : ledger) {
            DistLedgerCommonDefinitions.Operation.Builder operation = DistLedgerCommonDefinitions.Operation.newBuilder();
            operation.setUserId(op.getAccount()).setType(DistLedgerCommonDefinitions.OperationType.OP_UNSPECIFIED).addAllTS(op.getUpdateTS().toList()).addAllPrevTS(op.getPrevTS().toList());
            if (op instanceof CreateOp) {
                operation.setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT);
            } else if (op instanceof DeleteOp) {
                operation.setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT);
            } else if (op instanceof TransferOp) {
                String destUserId = ((TransferOp) op).getDestAccount();
                int amount = ((TransferOp) op).getAmount();
                operation.setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).setDestUserId(destUserId).setAmount(amount);
            }
            ledgerStateBuilder.addLedger(operation.build());
        }
        PropagateStateRequest request = PropagateStateRequest.newBuilder().setState(ledgerStateBuilder.build()).addAllReplicaTS(replicaTS).build();
        try {
            // Define a timeout of 2 seconds
            stub.withDeadlineAfter(2, TimeUnit.SECONDS).propagateState(request);
            return true;
        } catch (StatusRuntimeException e) {
            return false;
        }

        /* TODO: Remove this
        DistLedgerCommonDefinitions.Operation.Builder operation = DistLedgerCommonDefinitions.Operation.newBuilder();

        operation.setUserId(op.getAccount()).setType(DistLedgerCommonDefinitions.OperationType.OP_UNSPECIFIED);
        if (op instanceof CreateOp) {
            operation.setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT);
        } else if (op instanceof DeleteOp) {
            operation.setType(DistLedgerCommonDefinitions.OperationType.OP_DELETE_ACCOUNT);
        } else if (op instanceof TransferOp) {
            String destUserId = ((TransferOp) op).getDestAccount();
            int amount = ((TransferOp) op).getAmount();
            operation.setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).setDestUserId(destUserId).setAmount(amount);
        }
        DistLedgerCommonDefinitions.LedgerState ledger = DistLedgerCommonDefinitions.LedgerState.newBuilder().addLedger(operation.build()).build();
        PropagateStateRequest request = PropagateStateRequest.newBuilder().setState(ledger).build();
        try {
            // Define a timeout of 2 seconds after which the call will be aborted, and false will be returned
            PropagateStateResponse response = stub.withDeadlineAfter(2, TimeUnit.SECONDS).propagateState(request);
            return true;
        } catch (StatusRuntimeException e) {
            return false;
        }
        */
    }

    public void shutdownNow() {
        channel.shutdownNow();
    }

}
