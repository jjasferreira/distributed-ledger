package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

// Exceptions
import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) {
        System.out.println(ServerMain.class.getSimpleName());

        // receive and print arguments
        // TODO: add debug flag
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 1) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
			return;
		}

		final int port = Integer.parseInt(args[0]);
		final BindableService usrImpl = new UserServiceImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(usrImpl).build();

		// Start the server
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

        // create server socket

    }

}

