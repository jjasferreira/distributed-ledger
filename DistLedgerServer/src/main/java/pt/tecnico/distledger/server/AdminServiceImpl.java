package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.tecnico.distledger.server.domain.LedgerState;

import io.grpc.stub.StreamObserver;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ServerState state;

    public AdminServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        state.activate();
        ActivateResponse response = ActivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        state.deactivate();
        DeactivateResponse response = DeactivateResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLedgerState(GetLedgerStateRequest request, StreamObserver<GetLedgerStateResponse> responseObserver) {
        /*
        LedgerState ledger = state.getLedgerState();
        GetLedgerStateResponse response = GetLedgerStateResponse.newBuilder().setLedgerState(ledger).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        */
    }
}
