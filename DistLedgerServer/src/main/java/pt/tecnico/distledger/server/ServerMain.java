package pt.tecnico.distledger.server;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.NamingServerService;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;


public class ServerMain {

	private static final String NAMING_HOST = "localhost";
	private static final String NAMING_PORT = "5001";

	private static final String SERVICE_NAME = "DistLedger";
	private static final String SERVER_HOST = "localhost";

	private static final boolean debug = (System.getProperty("debug") != null);
    public static void main(String[] args) {

        System.out.println(ServerMain.class.getSimpleName());

        // Receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// Check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		// Instantiate a new server state and service implementations
		final int port = Integer.parseInt(args[0]);
		final String role = args[1];

		if (port < 1024 || port > 65353) {
			System.err.println("Port number must be between 1024 and 65563");
			return;
		}

		if (!role.equals("A") && !role.equals("B")) {
			System.err.println("Role must be either A or B");
			return;
		}

		final String address = SERVER_HOST + ":" + port;

		final NamingServerService namingServerService = new NamingServerService(NAMING_HOST, NAMING_PORT);

		ServerState state = new ServerState(debug, namingServerService, role, SERVICE_NAME);

		final BindableService userImpl = new UserServiceImpl(state);
		final BindableService adminImpl = new AdminServiceImpl(state);

		// Create a new server to listen on port and start it
		Server server = ServerBuilder.forPort(port).addService(userImpl).addService(adminImpl).build();
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Server started");

		// Register server to known naming server
		state.registerToNamingServer(SERVICE_NAME, role, address);

		// Do not exit the main thread. Wait until server is terminated.
		try {
			server.awaitTermination();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// Delete server from known naming server
			state.deleteFromNamingServer(SERVICE_NAME, address);
		}

    }

}
