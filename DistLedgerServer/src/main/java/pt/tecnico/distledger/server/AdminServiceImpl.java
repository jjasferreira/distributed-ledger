package pt.tecnico.distledger.server;

import com.google.longrunning.ListOperationsResponse;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

import io.grpc.stub.StreamObserver;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final ServerState state;

    public AdminServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {t
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
        /*
        LedgerState ledger = state.getLedgerState();
        GetLedgerStateResponse response = GetLedgerStateResponse.newBuilder().setLedgerState(ledger).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        */
        List<Operation> ledger = state.getLedger();

        for (Operation operation : ledger) {
            DistLedgerCommonDefinitions.Operation.newBuilder().setType(operation.)
        }

    }
}
