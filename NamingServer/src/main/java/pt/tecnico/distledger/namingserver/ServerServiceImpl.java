package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.*;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.grpc.*;

public class ServerServiceImpl extends NamingServerServiceGrpc.ServerServiceImplBase {

    private final NamingServerState state;

    private boolean debug = false;

    public ServerServiceImpl(NamingServerState state, boolean debug) {
        this.state = state;
        this.debug = debug;
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
        }
        catch (Exception e) {
            if (e.getMessage().equals("ADDRESS_ALREADY_REGISTERED")) {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Account does not exist").asRuntimeException());
            } else {
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
                e.printStackTrace();
            }
        }

        }
        // TODO: synchronize this
        // TODO: if (debug) {
        //    System.err.println("Registering server " + name + " with role " + role + " and address " + address);
        //}
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        // get attributes from request

        String serviceName = request.getServiceName();
        String serverAddress = request.getAddress();

        try {
            state.removeServer(serviceName, serverAddress);
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
            e.printStackTrace();
        }
    }