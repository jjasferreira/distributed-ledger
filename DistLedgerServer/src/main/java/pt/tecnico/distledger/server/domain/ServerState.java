package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class ServerState {
    private final List<Operation> ledger;
    private final HashMap<String, Integer> accounts;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        accounts.put("broker", 1000);
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
        if (!accounts.containsKey(account)) {
            this.accounts.put(account, 0);
            ledger.add(new CreateOp(account));
            return 0;
        } else {
            return -1;
        }
    }

    public int deleteAccount(String account) {
        if (this.accounts.remove(account) != null) {
            ledger.add(new DeleteOp(account));
            return 0;
        }
        return -1;
    }

    public int getBalance(String account) {
        if (accounts.containsKey(account)) {
            return this.accounts.get(account);
        }
        return -1;
    }

    public int transferTo(String accountFrom, String accountTo, int amount) {
        if (!accounts.containsKey(accountFrom)) {
            return -1;
        }
        if (!accounts.containsKey(accountTo)) {
            return -2;
        }
        if (accounts.get(accountFrom) < amount) {
            return -3;
        }
        accounts.put(accountFrom, accounts.get(accountFrom) - amount);
        accounts.put(accountTo, accounts.get(accountTo) + amount);
        return 0;
    }

}