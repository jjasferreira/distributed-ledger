package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.NamingServerService;


public class AdminClientMain {

    private static final String NAMING_HOST = "localhost";
    private static final int NAMING_PORT = 5001;

    private static final boolean debug = (System.getProperty("debug") != null);

    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        final NamingServerService namingServerService = new NamingServerService(NAMING_HOST, NAMING_PORT);

        CommandParser parser = new CommandParser(debug, namingServerService);
        parser.parseInput();
    }
}
