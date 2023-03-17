package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.*;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.grpc.*;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private final NamingServerState namingServerState;

    public NamingServerServiceImpl(NamingServerState namingServerState) {
        this.namingServerState = namingServerState;
    }

    @Override
    public void register (RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String name = request.getServerName();
        String role = request.getServerRole();
        String address = request.getServerAddress();
        ServiceEntry serviceEntry = namingServerState.getService(name);
        if (serviceEntry == null) {
            namingServerState.addService(new ServiceEntry(name));
        }
        namingServerState.addService(name, role, address);
        responseObserver.onNext(RegisterResponse.newBuilder().build());
        responseObserver.onCompleted();
        //TODO exception handling
    }

    @Override
    public void lookup (LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        //TODO
    }