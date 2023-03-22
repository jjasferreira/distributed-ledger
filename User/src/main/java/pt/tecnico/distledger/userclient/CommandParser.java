package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.*;

import io.grpc.StatusRuntimeException;

import java.util.Scanner;
import java.util.HashMap;


public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";


    private final NamingServerService namingServerService;

    HashMap<String, UserService> userServices = new HashMap<>();

    public CommandParser(NamingServerService namingServerService) {
        this.namingServerService = namingServerService;
        /*
        List<ServerEntry> servers = namingServerService.lookup();
        for (ServerEntry entry : servers) {
            if (primaryUserService == null && entry.getRole == "A") {
                String[] hostPort = entry.getAddress.split(":");
                primaryUserService = new UserService(hostPort[0], hostPort[1]);
            }
            if (secondaryUserService == null && entry.getRole == "B") {
                String[] hostPort = entry.getAddress.split(":");
                secondaryUserService = new UserService(hostPort[0], hostPort[1]);
            }

        }
        this.primaryUserService;
        */
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try{
                switch (cmd) {
                    case CREATE_ACCOUNT:
                        this.createAccount(line);
                        break;

                    case DELETE_ACCOUNT:
                        this.deleteAccount(line);
                        break;

                    case TRANSFER_TO:
                        this.transferTo(line);
                        break;

                    case BALANCE:
                        this.balance(line);
                        break;

                    case HELP:
                        this.printUsage();
                        break;

                    case EXIT:
                        //userService.shutdownNow();
                        exit = true;
                        break;

                    default:
                        System.err.println("Invalid command");
                        break;
                }
            }
            catch (StatusRuntimeException e) {
                System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private boolean lookup(String name, String role) {
        HashMap<String, String> servers = namingServerService.lookup(name, role);

        if (servers.isEmpty()) {
            return false;
        }
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            if (!userServices.containsKey("A") && entry.getValue().equals("A")) {
                String[] address = entry.getKey().split(":", 2);
                UserService userService = new UserService(address[0], Integer.parseInt(address[1]));
                this.userServices.put("A", userService);
            }
            if (!userServices.containsKey("B") && entry.getValue().equals("B")) {
                String[] address = entry.getKey().split(":", 2);
                UserService userService = new UserService(address[0], Integer.parseInt(address[1]));
                this.userServices.put("B", userService);
            }
        }
        return true;
    }

    private void createAccount(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                System.err.println("No server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).createAccount(username);
        System.out.println("OK");
        System.out.println(response);
    }

    private void deleteAccount(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 3){
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                System.err.println("No server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).deleteAccount(username);
        System.out.println("OK");
        System.out.println(response);
    }

    private void balance(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 3){
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                System.err.println("No server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).balance(username);
        System.out.println("OK");
        System.out.println(response);
    }

    private void transferTo(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 5){
            this.printUsage();
            return;
        }
        String role = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                System.err.println("No server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).transferTo(from, dest, amount);
        System.out.println("OK");
        System.out.println(response);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                        "- createAccount <server> <username>\n" +
                        "- deleteAccount <server> <username>\n" +
                        "- balance <server> <username>\n" +
                        "- transferTo <server> <username_from> <username_to> <amount>\n" +
                        "- exit\n");
    }
}