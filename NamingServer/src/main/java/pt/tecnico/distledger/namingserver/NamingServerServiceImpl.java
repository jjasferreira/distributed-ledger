package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.domain.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;

import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.stub.StreamObserver;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private final NamingServerState state;

    public NamingServerServiceImpl(NamingServerState state) {
        this.state = state;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String name = request.getServiceName();
        String role = request.getRole();
        String address = request.getAddress();
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
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String name = request.getServiceName();
        String role = request.getRole();
        try {
            state.lookup(name, role);
            LookupResponse response = LookupResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            // TODO: are there no custom exceptions to be caugth?
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String name = request.getServiceName();
        String address = request.getAddress();
        try {
            // CCCCCCAUTION voces antes tinham state.removeServer(name, address);??? RRRRRRRREEEEEEEAAAAADDDDMMMMEEEEE ps Andre delete this comment
            state.delete(name, address);
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