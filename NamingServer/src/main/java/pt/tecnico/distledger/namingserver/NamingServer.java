package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.domain.*;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import static java.lang.System.exit;


public class NamingServer {

    private static final boolean debug = (System.getProperty("debug") != null);

    private static final int PORT = 5001;

    public static void main(String[] args) {

        System.out.println(NamingServer.class.getSimpleName());

        // Instantiate a new server state and service implementations
        NamingServerState state = new NamingServerState(debug);
        final BindableService namingServerImpl = new NamingServerServiceImpl(state);

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(PORT).addService(namingServerImpl).build();

        // Start the server
        try {
            server.start();
            System.out.println("Naming Server started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Do not exit the main thread. Wait until server is terminated.
        try {
            System.out.println("Press enter to shutdown");
            System.in.read();
            if (debug)
                System.err.println("[DEBUG] Shutting down...");
            server.shutdown();
            if (debug)
                System.err.println("[DEBUG] OK");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            exit(0);
        }
    }
}
