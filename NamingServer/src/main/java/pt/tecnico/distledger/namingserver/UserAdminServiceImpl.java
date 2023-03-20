package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.*;

import io.grpc.stub.StreamObserver;

import pt.tecnico.distledger.grpc.*;

public class UserAdminServiceImpl extends UserAdminServiceGrpc.UserAdminServiceImplBase {

    private final NamingServerState state;

    public NamingServerServiceImpl(NamingServerState state) {
        this.state = state;
    }


    @Override
    public void lookup (LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        String serviceName = request.getServiceName();
        String serviceRole = request.getServerRole();

        LookupResponse.Builder lookupResponseBuilder = LookupResponse.newBuilder();

        ServiceEntry serviceEntry = state.getService(serviceName);
        if (serviceEntry != null) {
            List<ServerEntry> serverEntryList = serviceEntry.getServers();
            if (!serverEntryList.isEmpty()) {
                for (ServerEntry entry: serverEntryList)
                    lookupResponseBuilder.addServerAddress(entry.getAddress())
            }
        }
        LookupResponse lookupResponse = lookupResponseBuilder.build();
        responseObserver.onNext()lookupResponse;feresponseObserver.onCompleted();

    }