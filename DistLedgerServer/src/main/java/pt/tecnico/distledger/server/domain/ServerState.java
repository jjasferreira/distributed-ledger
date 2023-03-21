package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import java.util.concurrent.locks.*;

public class ServerState {

    private final List<Operation> ledger;

    private final HashMap<String, Integer> accounts;

    private final String serviceName;

    private final boolean debug;

    private boolean isPrimary;

    private boolean active;

    private NamingServerService namingServerService;

    private CrossServerDistLedgerService crossServerService;

    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug, NamingServerService namingServerService, String role, String serviceName) {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.debug = debug;
        this.active = true;
        this.serviceName = serviceName;
        this.namingServerService = namingServerService;
        this.isPrimary = (role.equals("A"));
        if (isPrimary) this.setCrossServerService();
        accounts.put("broker", 1000);
    }

    public void registerToNamingServer(String name, String role, String address) {
        try {
            namingServerService.register(SERVICE_NAME, role, address);
            System.out.println("Server registered to naming server");
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void deleteFromNamingServer(String name, String address) {
        try {
            namingServerService.register(SERVICE_NAME, address);
            System.out.println("Server deleted from naming server");
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void setCrossServerService() {
        Map<String, String> servers = namingServerService.lookup(serviceName, "B");
        for (Map.Entry<String, String> entry : servers.entrySet()) {
            if (entry.getValue().equals("B")) {
            this.crossServerService = new crossServerService(entry.getKey());
        }
    }

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }

    public boolean isPrimary() {
        return isPrimary;
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
            state.registerToNamingServer(SERVICE_NAME, role, address);
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
            state.deleteFromNamingServer(SERVICE_NAME, address);
        } finally {
            activeLock.writeLock().unlock();
        }
        debug("OK");
    }

    public List<Operation> getLedger() {
        debug("> Getting ledger state...");
        debug("OK");
        // All blocks that modify the ledger must have access to accounts
        // TODO: return copy of ledger
        synchronized (accounts) {
            return ledger;
        }
    }

    public void gossip() {
        // TODO: next phases
    }

    public void createAccount(String account) throws ServerStateException {
        activeLock.readLock().lock();
        debug("> Creating account " + account + "...");
        if (!isPrimary) {
            // We can assume that request is well-formed and that server is active because otherwise the primary server
            // wouldn't have propagated the state
            synchronized(accounts) {
                accounts.put(account, 0);
                ledger.add(new CreateOp(account));
            }
            return;
        }
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
                accounts_copy = new HashMap<String, Integer>(accounts);
                ledger_copy = new ArrayList<Operation>(ledger)

                accounts_copy.put(account, 0);
                ledger_copy.add(new CreateOp(account));

                //TODO check if cached server is working
                // if (!crossServerService.ping()) this.setCrossServerService();

                if (crossServerService.propagateState(ledger_copy)) {
                    this.ledger = ledger_copy;
                    this.accounts = accounts_copy;
                }

            }
            debug("OK");
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void deleteAccount(String account) throws ServerStateException {
        debug("> Deleting account " + account + "...");
        if (!isPrimary) {
            // We can assume that request is well-formed and that server is active because otherwise the primary server
            // wouldn't have propagated the state
            synchronized(accounts) {
                accounts.remove(account);
                ledger.add(new DeleteOp(account));
            }
            return;
        }
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new ServerStateException("INACTIVE");
            }
            if (account.equals("broker")) {
                debug("NOK: " + account + " is the broker account");
                throw new ServerStateException("IS_BROKER");
            }
            synchronized (accounts) {
                if (!accounts.containsKey(account)) {
                    debug("NOK: " + account + " does not exist");
                    throw new ServerStateException("DOES_NOT_EXIST");
                }
                if (accounts.get(account) > 0) {
                    debug("NOK: " + account + " still has money");
                    throw new ServerStateException("HAS_MONEY");
                }
                //TODO: these can be shallow copies, right?
                accounts_copy = new HashMap<String, Integer>(accounts);
                ledger_copy = new ArrayList<Operation>(ledger)

                accounts_copy.remove(account);
                ledger_copy.add(new DeleteOp(account));
                //TODO check if cached server is working
                // if (!crossServerService.ping()) this.setCrossServerService();

                if (crossServerService.propagateState(ledger_copy)) {
                    this.ledger = ledger_copy;
                    this.accounts = accounts_copy;
                    //TODO: is there a way to delete the original ledgers?
                }
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
                if (!isPrimary) {
                    accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                    accounts.put(accountTo, accounts.get(accountTo) + amount);
                    ledger.add(new TransferOp(accountFrom, accountTo, amount));
                } else {
                    accounts_copy = new HashMap<String, Integer>(accounts);
                    ledger_copy = new ArrayList<Operation>(ledger);
                    accounts_copy.put(accountFrom, accounts_copy.get(accountFrom) - amount);
                    accounts_copy.put(accountTo, accounts_copy.get(accountTo) + amount);
                    ledger_copy.add(new TransferOp(accountFrom, accountTo, amount));

                    //TODO check if cached server is working
                    // if (!crossServerService.ping()) this.setCrossServerService();

                    if (crossServerService.propagateState(ledger_copy)) {
                        this.ledger = ledger_copy;
                        this.accounts = accounts_copy;
                        //TODO: is there a way to delete the original ledgers?
                    }
                }
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

}
