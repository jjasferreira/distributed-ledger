package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.NamingServerService;


public class UserClientMain {

    private static final String NAMING_HOST = "localhost";
    private static final int NAMING_PORT = 5001;

    private static final boolean debug = (System.getProperty("debug") != null);

    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        final NamingServerService namingServerService = new NamingServerService(NAMING_HOST, NAMING_PORT);

        CommandParser parser = new CommandParser(debug, namingServerService);
        parser.parseInput();
    }
}
