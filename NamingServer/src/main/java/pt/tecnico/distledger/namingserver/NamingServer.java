package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.domain.*;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

// Exceptions
import java.io.IOException;

public class NamingServer {

    private static final boolean debug = (System.getProperty("debug") != null);

    public static void main(String[] args) {

        // TODO: The naming server listens for connections on port 5001

        System.out.println(NamingServer.class.getSimpleName());

        // Receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // Check arguments
        if (args.length > 1) {
            System.err.println("Too many arguments!");
            System.err.printf("Usage: java %s", NamingServer.class.getName());
            return;
        }

        // Instantiate a new server state and service implementations
        NamingServerState state = new NamingServerState(debug);
        final int port = Integer.parseInt(args[0]);
        final BindableService namingServerImpl = new NamingServerServiceImpl(state);

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(namingServerImpl).build();

        // Start the server TODO reconsider exception handling
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Server threads are running in the background.
        System.out.println("Naming Server started");

        // Do not exit the main thread. Wait until server is terminated.
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
