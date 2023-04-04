package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.*;

import io.grpc.StatusRuntimeException;

import java.util.Scanner;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;


public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final boolean debug;

    private final NamingServerService namingServerService;

    private List<Integer> prevTS;

    HashMap<String, UserService> userServices = new HashMap<>();

    public CommandParser(boolean debug, NamingServerService namingServerService) {
        this.debug = debug;
        this.namingServerService = namingServerService;
        this.prevTS = new ArrayList<>(Collections.nCopies(5, 0)); // 5 servers max
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
                        for (HashMap.Entry<String, UserService> service : userServices.entrySet())
                            service.getValue().shutdownNow();
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

    private void debug(String message) {
        if (this.debug)
            System.err.println("[DEBUG] " + message);
    }

    public void updateLocalTimestamp(List<Integer> newTS) {
        debug("OK:" + this.prevTS + "-->" + newTS);
        // Create a new list and copy the elements from newTS
        List<Integer> copiedTS = new ArrayList<>(newTS);
        int sizeTS = copiedTS.size();
        for (int i = 0; i < sizeTS; i++) {
            // Update the element in prevTS with the value from newTS if it is greater
            if (copiedTS.get(i) > prevTS.get(i))
                this.prevTS.set(i, copiedTS.get(i));
        }
        debug("OK: updated local timestamp to " + this.prevTS);
    }

    private boolean lookup(String name, String role) {
        // Lookup for servers with the given service and role. Returns true if a server with the given role is found
        debug("> Looking for available servers with service " + name + " and role " + role + "...");
        HashMap<String, String> servers = namingServerService.lookup(name, role);
        if (servers.isEmpty()) {
            debug("NOK: no server found");
            return false;
        }
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            String[] address = entry.getKey().split(":", 2);
            UserService userService = new UserService(address[0], Integer.parseInt(address[1]));
            this.userServices.put(entry.getValue(), userService);
        }
        if (this.userServices.containsKey(role)) {
            debug("OK: found server with role " + role);
            return true;
        }
        return false;
    }

    private void createAccount(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 3) {
            this.printUsage();
            debug("NOK: unexpected number of arguments");
            return;
        }
        String role = split[1];
        String username = split[2];
        debug("> Creating account " + username + " on server with role " + role + "...");
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        List<Integer> newTS = userServices.get(role).createAccount(username, prevTS);
        updateLocalTimestamp(newTS);
        System.out.println("OK\n");
    }

    private void balance(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 3) {
            this.printUsage();
            return;
        }
        String role = split[1];
        String username = split[2];
        debug("> Getting balance of account " + username + " from server with role " + role + "...");
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: No server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        BalanceInfo info = userServices.get(role).balance(username, prevTS);
        updateLocalTimestamp(info.getValueTS());
        System.out.println("OK\n" + info.getValue() + "\n");
    }

    private void transferTo(String line) {
        String[] split = line.split(SPACE);
        if (split.length != 5) {
            this.printUsage();
            return;
        }
        String role = split[1];
        String from = split[2];
        String to = split[3];
        Integer amount = Integer.valueOf(split[4]);
        debug("> Transferring " + amount + " from account " + from + " to account " + to + " on server with role " + role + "...");
        if (!userServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        List<Integer> newTS = userServices.get(role).transferTo(from, to, amount, prevTS);
        updateLocalTimestamp(newTS);
        System.out.println("OK\n");
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                        "- createAccount <server> <username>\n" +
                        "- balance <server> <username>\n" +
                        "- transferTo <server> <username_from> <username_to> <amount>\n" +
                        "- exit\n");
    }

}
