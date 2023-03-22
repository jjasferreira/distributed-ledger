package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.NamingServerService;
import pt.tecnico.distledger.adminclient.grpc.AdminService;


public class AdminClientMain {

    private static final String NAMING_HOST = "localhost";
    private static final int NAMING_PORT = 5001;

    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        CommandParser parser = new CommandParser(new AdminService("localhost", NAMING_PORT));
        parser.parseInput();
    }
}
