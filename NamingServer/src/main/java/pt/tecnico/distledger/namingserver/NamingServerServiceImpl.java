package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.*;
import pt.tecnico.distledger.grpc.*;

import io.grpc.stub.StreamObserver;


public class NamingServerServiceImpl extends NamingServerServiceGrpc.ServerServiceImplBase {

    private final NamingServerState state;

    public ServerServiceImpl(NamingServerState state) {
        this.state = state;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String name = request.getServiceName();
        String role = request.getServerRole();
        String address = request.getServerAddress();
        try {
            state.register(name, role, address);
            RegisterResponse response = RegisterResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (e.getMessage().equals("ADDRESS_ALREADY_REGISTERED")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("This address is already registered for this service").asRuntimeException());
            } else if (e.getMessage().equals("ROLE_ALREADY_REGISTERED")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("This role is already registered for this service").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String name = request.getServiceName();
        String address = request.getAddress();
        try {
            state.removeServer(name, address);
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (e.getMessage().equals("SERVICE_DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("The service does not exist").asRuntimeException());
            } else if (e.getMessage().equals("ADDRESS_DOES_NOT_EXIST")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("The address does not exist for this service").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }
    }

}