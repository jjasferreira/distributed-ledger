package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class ServerState {
    private final List<Operation> ledger;
    private final HashMap<String, Integer> accounts;

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        accounts.put("Broker", 1000f);
    }

    public void addOperation(Operation op) {
        this.ledger.add(op);
    }

    public List<Operation> getLedger() {
        return this.ledger;
    }

    public HashMap<String, Integer> getAccounts() {
        return this.accounts;
    }

    public void addAccount(String account, Integer balance) {
        if (accounts.containsKey(account)) {
            throw new IllegalArgumentException("Account already exists");
        }
        this.accounts.put(account, balance);
    }

    public int getBalance(String account) {
        if (!accounts.containsKey(account)) {
            throw new IllegalArgumentException("Account does not exist");
        }
        return this.accounts.get(account);
    }

    public List<Operation> getOperations(String account) {
        if (!accounts.containsKey(account)) {
            throw new IllegalArgumentException("Account does not exist");
        }
        List<Operation> operations = new ArrayList<>();
        for (Operation op : this.ledger) {
            if (op.getAccount().equals(account)) {
                operations.add(op);
            }
        }
        return operations;
    }

}
