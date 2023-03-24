package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.domain.*;
import pt.tecnico.distledger.namingserver.domain.exception.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServer.*;

import io.grpc.stub.StreamObserver;
import static io.grpc.Status.*;

import java.util.List;


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
        } catch (AlreadyRegisteredAddressException | AlreadyRegisteredRoleException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String name = request.getServiceName();
        String role = request.getRole();
        try {
            List<ServerEntry> servers = state.lookup(name, role);
            ServerList.Builder serverList = NamingServer.ServerList.newBuilder();
            for (ServerEntry serverEntry : servers) {
                role = serverEntry.getRole();
                String address = serverEntry.getAddress();
                Server server = NamingServer.Server.newBuilder().setRole(role).setAddress(address).build();
                serverList.addServer(server);
            }
            LookupResponse response = LookupResponse.newBuilder().setServers(serverList.build()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        String name = request.getServiceName();
        String address = request.getAddress();
        try {
            state.delete(name, address);
            DeleteResponse response = DeleteResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NonExistingServiceException | NonExistingAddressException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
            e.printStackTrace();
        }
    }

}