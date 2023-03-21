package pt.tecnico.distledger.server.grpc;

import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.CrossServerDistLedger.*;
import pt.tecnico.distledger.server.domain.operation.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;


public class CrossServerService {

    final ManagedChannel channel;

    CrossServerServiceGrpc.CrossServerServiceBlockingStub stub;

    public CrossServerService(String host, int port) {
        final String target = host + ":" + port;
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = NamingServerServiceGrpc.newBlockingStub(channel);
    }

    public String propagateState(List<Operation> ledger) {
        PropagateStateRequest request = PropagateStateRequest.newBuilder().setState(ledger).build();
        PropagateStateResponse response = stub.propagateState(request);
        return response.toString();
    }

    public void shutdownNow() {
        channel.shutdownNow();
    }
}
