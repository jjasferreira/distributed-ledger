package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

// Exceptions
import java.io.IOException;

public class ServerMain {

	private static final String DEBUG_FLAG = "-debug";
    public static void main(String[] args) {

        System.out.println(ServerMain.class.getSimpleName());

		boolean debug = false;

        // Receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
			if (args[i].equals(DEBUG_FLAG)) {
				debug = true;
			}
		}

		// Check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		// Instantiate a new server state and service implementations
		ServerState state = new ServerState(debug);
		final int port = Integer.parseInt(args[0]);
		final BindableService userImpl = new UserServiceImpl(state);
		final BindableService adminImpl = new AdminServiceImpl(state);

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(userImpl).addService(adminImpl).build();

		// Start the server TODO reconsider exception handling
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Server threads are running in the background.
		System.out.println("Server started");

		// Do not exit the main thread. Wait until server is terminated.
		try {
			server.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

    }

}
