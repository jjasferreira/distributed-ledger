// package pt.tecnico.distledger.namingserver;

// import pt.tecnico.distledger.namingserver.*;
// import pt.tecnico.distledger.grpc.*;

// import io.grpc.stub.StreamObserver;


// public class UserAdminServiceImpl extends UserAdminServiceGrpc.UserAdminServiceImplBase {

//     private final NamingServerState state;

//     public NamingServerServiceImpl(NamingServerState state) {
//         this.state = state;
//     }

//     @Override
//     public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
//         String name = request.getServiceName();
//         String role = request.getServerRole();
//         try {
//             state.lookup(name, role);
//             LookupResponse response = LookupResponse.newBuilder().build();
//             responseObserver.onNext(response);
//             responseObserver.onCompleted();
//         } catch (Exception e) {
//             // TODO: are there no custom exceptions to be caugth?
//             responseObserver.onError(INVALID_ARGUMENT.withDescription("Unknown error").asRuntimeException());
//             e.printStackTrace();
//         }
//     }

// }