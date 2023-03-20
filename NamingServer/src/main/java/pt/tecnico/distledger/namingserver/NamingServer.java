package pt.tecnico.distledger.namingserver;

public class NamingServer {

    public static void main(String[] args) {

        private static final boolean debug = (System.getProperty("debug") != null);

        // TODO: The naming server listens for connections on port 5001

        System.out.println(ServerMain.class.getSimpleName());

        // Receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // Check arguments
        if (args.length > 0) {
            System.err.println("Too many arguments!");
            System.err.printf("Usage: java %s", NamingServer.class.getName());
            return;
        }

        // Instantiate a new server state and service implementations
        NamingServerState state = new NamingServerState(debug);
        final int port = Integer.parseInt(args[0]);
        final BindableService namingServerImpl = new NamingServerImpl(state);

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
