package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.*;

import io.grpc.StatusRuntimeException;

import java.util.Scanner;
import java.util.HashMap;


public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final boolean debug;

    private final NamingServerService namingServerService;

    HashMap<String, AdminService> adminServices = new HashMap<>();

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

            try {
                switch (cmd) {
                    case ACTIVATE:
                        this.activate(line);
                        break;

                    case DEACTIVATE:
                        this.deactivate(line);
                        break;

                    case GET_LEDGER_STATE:
                        this.dump(line);
                        break;

                    case GOSSIP:
                        this.gossip(line);
                        break;

                    case HELP:
                        this.printUsage();
                        break;

                    case EXIT:
                        namingServerService.shutdownNow();
                        for (HashMap.Entry<String, AdminService> entry : adminServices.entrySet()) {
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

    private void debug(String message) {
        if (this.debug)
            System.err.println("[DEBUG] " + message);
    }

    private boolean lookup(String name, String role) {
        debug("> Looking for available servers with service " + name + " and role " + role + "...");
        HashMap<String, String> servers = namingServerService.lookup(name, role);
        if (servers.isEmpty()) {
            debug("NOK: no server found");
            return false;
        }
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            if (!adminServices.containsKey("A") && entry.getValue().equals("A")) {
                String[] address = entry.getKey().split(":", 2);
                AdminService adminService = new AdminService(address[0], Integer.parseInt(address[1]));
                this.adminServices.put("A", adminService);
            }
            if (!adminServices.containsKey("B") && entry.getValue().equals("B")) {
                String[] address = entry.getKey().split(":", 2);
                AdminService adminService = new AdminService(address[0], Integer.parseInt(address[1]));
                this.adminServices.put("B", adminService);
            }
        }
        debug("OK");
        return true;
    }

    private void activate(String line) {
        String role = lineParse(line);
        if (role == null)
            return;
        debug("> Activating server with role " + role + "...");
        if (!adminServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        String response = adminServices.get(role).activate();
        debug("OK");
        System.out.println("OK");
        System.out.println(response);
    }

    private void deactivate(String line) {
        String role = lineParse(line);
        if (role == null)
            return;
        debug("> Deactivating server with role " + role + "...");
        if (!adminServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        String response = adminServices.get(role).deactivate();
        debug("OK");
        System.out.println("OK");
        System.out.println(response);
    }

    private void dump(String line){
        String role = lineParse(line);
        if (role == null)
            return;
        debug("> Getting ledger state of server with role " + role + "...");
        if (!adminServices.containsKey(role)) {
            if (!this.lookup("DistLedger", role)) {
                debug("NOK: no server with given role available to handle request");
                System.err.println("No server available");
                return;
            }
        }
        String response = adminServices.get(role).getLedgerState();
        debug("OK");
        System.out.println("OK");
        System.out.println(response);
    }

    private void gossip(String line) {
        System.out.println("TODO: implement gossip command (only for phase 3)");
    }

    private String lineParse(String line){
        String[] split = line.split(SPACE);
        if (split.length != 2){
            this.printUsage();
            debug("NOK: unexpected number of arguments");
            return null;
        }
        return split[1];
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <server>\n" +
                "- exit\n");
    }

}
