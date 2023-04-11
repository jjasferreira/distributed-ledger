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

    public boolean propagateState(List<Operation> ledger, String replicaRole, List<Integer> replicaTS) {
        DistLedgerCommonDefinitions.LedgerState.Builder ledgerStateBuilder = DistLedgerCommonDefinitions.LedgerState.newBuilder();
        for (Operation op : ledger) {
            DistLedgerCommonDefinitions.Operation.Builder operation = DistLedgerCommonDefinitions.Operation.newBuilder();
            operation.setUserId(op.getAccount()).setType(DistLedgerCommonDefinitions.OperationType.OP_UNSPECIFIED).setReplicaIndex(op.getReplicaIndex()).addAllTS(op.getUpdateTS().toList()).addAllPrevTS(op.getPrevTS().toList());
            if (op instanceof CreateOp) {
                operation.setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT);
            } else if (op instanceof TransferOp) {
                String destUserId = ((TransferOp) op).getDestAccount();
                int amount = ((TransferOp) op).getAmount();
                operation.setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).setDestUserId(destUserId).setAmount(amount);
            }
            ledgerStateBuilder.addLedger(operation.build());
        }
        PropagateStateRequest request = PropagateStateRequest.newBuilder().setState(ledgerStateBuilder.build()).setReplicaRole(replicaRole).addAllReplicaTS(replicaTS).build();
        try {
            // Define a timeout of 2 seconds
            stub.withDeadlineAfter(2, TimeUnit.SECONDS).propagateState(request);
            return true;
        } catch (StatusRuntimeException e) {
            return false;
        }
    }

    public void shutdownNow() {
        channel.shutdownNow();
    }

}
