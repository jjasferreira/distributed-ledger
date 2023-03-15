package pt.tecnico.distledger.adminclient;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.adminclient.grpc.AdminService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;
    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
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
                        adminService.shutdownNow();
                        exit = true;
                        break;

                    default:
                        System.err.println("Invalid command");
                        break;
                }
            }
            catch (StatusRuntimeException e) {
                System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void activate(String line){
        String[] split = line.split(SPACE);
        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        String response = adminService.activate(server);
        System.out.println("OK");
        System.out.println(response);
    }

    private void deactivate(String line){
        String[] split = line.split(SPACE);
        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        String response = adminService.deactivate(server);
        System.out.println("OK");
        System.out.println(response);
    }

    private void dump(String line){
        String[] split = line.split(SPACE);
        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        String response = adminService.getLedgerState(server);
        System.out.println("OK");
        System.out.println(response);
    }

    private void gossip(String line){
        //TODO: next phases
        System.out.println("TODO: implement gossip command (only for phase 3)");
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
