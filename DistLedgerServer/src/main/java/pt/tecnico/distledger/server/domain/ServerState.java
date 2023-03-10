package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import java.util.concurrent.locks.*;

public class ServerState {

    private final HashMap<String, Integer> accounts;

    private final List<Operation> ledger;

    private Boolean active;

    private final boolean debug;

    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug) {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.debug = debug;
        this.active = true;
        accounts.put("broker", 1000);
    }

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }

    public void activate() throws ServerStateException {
        debug("> Activating server...");
        activeLock.writeLock().lock();
        try {
            if (active) {
                debug("NOK: server already active");
                throw new ServerStateException("ALREADY_ACTIVE");
            }
            active = true;
        } finally {
            activeLock.writeLock().unlock();
        }
        debug("OK");
    }

    public void deactivate() throws ServerStateException {
        debug("> Deactivating server...");
        activeLock.writeLock().lock();
        try {
            if (!active) {
                debug("NOK: server already inactive");
                throw new ServerStateException("ALREADY_INACTIVE");
            }
            active = false;
        } finally {
            activeLock.writeLock().unlock();
        }
        debug("OK");
    }

    public List<Operation> getLedger() {
        debug("> Getting ledger state...");
        debug("OK");
        return ledger;
    }

    public void gossip() {
        // TODO: next phases
    }

    public void createAccount(String account) throws ServerStateException {
        activeLock.readLock().lock();
        debug("> Creating account " + account + "...");
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new ServerStateException("INACTIVE");
            }
            synchronized (accounts) {
                if (accounts.containsKey(account)) {
                    debug("NOK: " + account + " already exists");
                    throw new ServerStateException("ALREADY_EXISTS");
                }
                accounts.put(account, 0);
                ledger.add(new CreateOp(account));
            }
            debug("OK");
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void deleteAccount(String account) throws ServerStateException {
        debug("> Deleting account " + account + "...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new ServerStateException("INACTIVE");
            }
            //TODO which verifications don't need to be synchronized?
            synchronized (accounts) {
                if (account.equals("broker")) {
                    debug("NOK: " + account + " is the broker account");
                    throw new ServerStateException("IS_BROKER");
                }
                if (!accounts.containsKey(account)) {
                    debug("NOK: " + account + " does not exist");
                    throw new ServerStateException("DOES_NOT_EXIST");
                }
                if (accounts.get(account) > 0) {
                    debug("NOK: " + account + " still has money");
                    throw new ServerStateException("HAS_MONEY");
                }
                accounts.remove(account);
                ledger.add(new DeleteOp(account));
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

    public int getBalance(String account) throws ServerStateException {
        debug("> Getting balance of account " + account + "...");
        int balance;
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new ServerStateException("INACTIVE");
            }
            synchronized (accounts) {
                if (!accounts.containsKey(account)) {
                    debug("NOK: " + account + " does not exist");
                    throw new ServerStateException("DOES_NOT_EXIST");
                }
                balance = accounts.get(account);
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK: " + balance);
        return balance;
    }

    public void transferTo(String accountFrom, String accountTo, int amount) throws ServerStateException {
        debug("> Transferring " + amount + " from " + accountFrom + " to " + accountTo);
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new ServerStateException("INACTIVE");
            }
            if (accountFrom.equals(accountTo)) {
                debug("NOK: " + accountFrom + " and " + accountTo + " are the same account");
                throw new ServerStateException("SAME_ACCOUNT");
            }
            if (amount <= 0) {
                debug("NOK: " + amount + " is not a valid amount");
                throw new ServerStateException("INVALID_AMOUNT");
            }
            synchronized (accounts) {
                if (!accounts.containsKey(accountFrom)) {
                    debug("NOK: " + accountFrom + " does not exist");
                    throw new ServerStateException("FROM_DOES_NOT_EXIST");
                }
                if (!accounts.containsKey(accountTo)) {
                    debug("NOK: " + accountTo + " does not exist");
                    throw new ServerStateException("TO_DOES_NOT_EXIST");
                }
                if (accounts.get(accountFrom) < amount) {
                    debug("NOK: " + accountFrom + " does not have enough money");
                    throw new ServerStateException("NOT_ENOUGH_MONEY");
                }
                accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                accounts.put(accountTo, accounts.get(accountTo) + amount);
                ledger.add(new TransferOp(accountFrom, accountTo, amount));
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

}
