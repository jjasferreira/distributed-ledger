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

    private final boolean debug;

    private final NamingServerService namingServerService;

    HashMap<String, UserService> userServices = new HashMap<>();

    public CommandParser(boolean debug, NamingServerService namingServerService) {
        this.debug = debug;
        this.namingServerService = namingServerService;
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
                        namingServerService.shutdownNow();
                        for (HashMap.Entry<String, UserService> entry : userServices.entrySet()) {
                            entry.getValue().shutdownNow();
                        }
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

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }

    private boolean lookup(String name, String role) {
        debug("> Looking for available servers with service " + name + " and role " + role + "...");
        HashMap<String, String> servers = namingServerService.lookup(name, role);

        if (servers.isEmpty()) {
            debug("NOK: no server found");
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

        debug("OK");
        return true;
    }

    private void createAccount(String line) {
        debug("> Creating account with command \"" + line + "\"...");
        String[] split = line.split(SPACE);
        if (split.length != 3) {
            this.printUsage();
            debug("NOK: number of arguments unexpected");
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).createAccount(username);
        System.out.println("OK");
        System.out.println(response);
        debug("OK");
    }

    private void deleteAccount(String line) {
        debug("> Deleting account with command \"" + line + "\"...");
        String[] split = line.split(SPACE);
        if (split.length != 3){
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).deleteAccount(username);
        System.out.println("OK");
        System.out.println(response);
        debug("OK");
    }

    private void balance(String line) {
        debug("> Getting balance with command \"" + line + "\"...");
        String[] split = line.split(SPACE);
        if (split.length != 3){
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).balance(username);
        System.out.println("OK");
        System.out.println(response);
        debug("OK");
    }

    private void transferTo(String line) {
        debug("> Transferring with command \"" + line + "\"...");
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
                debug("NOK: no server available to handle request");
                return;
            }
        }
        String response = userServices.get(role).transferTo(from, dest, amount);
        System.out.println("OK");
        System.out.println(response);
        debug("OK");
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