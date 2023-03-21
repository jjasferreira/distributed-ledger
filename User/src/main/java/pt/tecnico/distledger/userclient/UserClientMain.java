package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;


public class UserClientMain {
    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        final String host = "localhost";
        final int port = 5001;

        CommandParser parser = new CommandParser(new NamingServerService(host, port));
        parser.parseInput();

    }
}
