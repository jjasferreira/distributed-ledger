package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class ServerState {
    private final List<Operation> ledger;
    private final HashMap<String, Integer> accounts;

    private boolean debug = false;

    public ServerState(boolean debug) {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.debug = debug;
        accounts.put("broker", 1000);
    }

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }


    public List<Operation> getLedger() {
        return this.ledger;
    }

    public HashMap<String, Integer> getAccounts() {
        return this.accounts;
    }
    public void addOperation(Operation op) {
        this.ledger.add(op);
    }

    public int createAccount(String account) {
        debug("Creating account " + account + "...");
        if (!accounts.containsKey(account)) {
            this.accounts.put(account, 0);
            ledger.add(new CreateOp(account));
            debug("OK");
            return 0;
        } else {
            debug("NOK: " + account + " already exists");
            return -1;
        }
    }

    public int deleteAccount(String account) {
        debug("Deleting account " + account + "...");
        if (accounts.remove(account) != null) {
            ledger.add(new DeleteOp(account));
            debug("OK");
            return 0;
        }
        debug("NOK: " + account + " does not exist");
        return -1;
    }

    public int getBalance(String account) {
        debug("Getting balance of account " + account + "...");
        if (accounts.containsKey(account)) {
            debug("OK: " + accounts.get(account));
            return this.accounts.get(account);
        }
        debug("NOK: " + account + " does not exist");
        return -1;
    }

    public int transferTo(String accountFrom, String accountTo, int amount) {
        debug("Transferring " + amount + " from " + accountFrom + " to " + accountTo);
        if (!accounts.containsKey(accountFrom)) {
            debug("NOK: " + accountFrom + " does not exist");
            return -1;
        }
        if (!accounts.containsKey(accountTo)) {
            debug("NOK: " + accountTo + " does not exist");
            return -2;
        }
        if (accounts.get(accountFrom) < amount) {
            debug("NOK: " + accountFrom + " does not have enough money");
            return -3;
        }
        if (amount < 0) {
            debug("NOK: " + amount + " is not a valid amount");
            return -4;
        }
        if (accountFrom.equals(accountTo)) {
            debug("NOK: " + accountFrom + " and " + accountTo + " are the same account");
            return -5;
        }
        accounts.put(accountFrom, accounts.get(accountFrom) - amount);
        accounts.put(accountTo, accounts.get(accountTo) + amount);
        ledger.add(new TransferOp(accountFrom, accountTo, amount));
        debug("OK");
        return 0;
    }

}
