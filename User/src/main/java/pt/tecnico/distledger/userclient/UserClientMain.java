package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.NamingServerService;


public class UserClientMain {

    private static final String NAMING_HOST = "localhost";
    private static final int NAMING_PORT = 5001;

    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new NamingServerService(NAMING_HOST, NAMING_PORT));
        parser.parseInput();
    }
}
