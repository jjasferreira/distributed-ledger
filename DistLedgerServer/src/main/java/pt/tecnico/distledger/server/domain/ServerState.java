package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.grpc.CrossServerService;

import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.locks.*;


public class ServerState {

    private final List<Operation> ledger;

    private final HashMap<String, Integer> accounts;

    private final String serviceName;

    private final String role;

    private final String address;

    private boolean active;

    private final boolean debug;

    private final NamingServerService namingServerService;

    private CrossServerService crossServerService;

    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug, NamingServerService namingServerService, String serviceName, String role, String address) {
        this.debug = debug;
        this.namingServerService = namingServerService;
        this.serviceName = serviceName;
        this.role = role;
        this.address = address;
        this.active = true;
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        if (role.equals("A"))
            this.setCrossServerService();
        accounts.put("broker", 1000);

    }

    public void registerToNamingServer(String name, String role, String address) {
        try {
            namingServerService.register(name, role, address);
            System.out.println("Server registered to naming server");
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void deleteFromNamingServer(String name, String address) {
        try {
            namingServerService.delete(name, address);
            System.out.println("Server deleted from naming server");
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void setCrossServerService() {
        HashMap<String, String> servers = namingServerService.lookup(serviceName, "B");
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            if (entry.getValue().equals("B")) {
                this.crossServerService = new CrossServerService(entry.getKey());
            }
        }
    }

    private void debug(String debugMsg) {
        if (this.debug)
            System.err.println("[DEBUG] " + debugMsg);
    }

    public boolean isPrimaryServer() {
        return role.equals("A");
    }

    public void activate() throws AlreadyActiveServerException {
        debug("> Activating server...");
        activeLock.writeLock().lock();
        try {
            if (active) {
                debug("NOK: server already active");
                throw new AlreadyActiveServerException();
            }
            active = true;
            this.registerToNamingServer(this.serviceName, this.role, this.address);
        } finally {
            activeLock.writeLock().unlock();
        }
        debug("OK");
    }

    public void deactivate() throws AlreadyInactiveServerException {
        debug("> Deactivating server...");
        activeLock.writeLock().lock();
        try {
            if (!active) {
                debug("NOK: server already inactive");
                throw new AlreadyInactiveServerException();
            }
            active = false;
            this.deleteFromNamingServer(this.serviceName, this.address);
        } finally {
            activeLock.writeLock().unlock();
        }
        debug("OK");
    }

    public List<Operation> getLedger() {
        debug("> Getting ledger state...");
        synchronized (accounts) {
            debug("OK");
            return new ArrayList<>(ledger);
        }
    }

    public void gossip() {
        // TODO: 3rd phase
    }

    public void createAccount(String account) throws InactiveServerException, AlreadyExistingAccountException {
        debug("> Creating account " + account + "...");
        if (!isPrimaryServer()) {
            // We can assume that request is well-formed and that server is active because otherwise the primary server
            // wouldn't have propagated the state
            synchronized(accounts) {
                accounts.put(account, 0);
                ledger.add(new CreateOp(account));
            }
            return;
        }
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            synchronized (accounts) {
                if (accounts.containsKey(account)) {
                    debug("NOK: " + account + " already exists");
                    throw new AlreadyExistingAccountException(account);
                }
                accounts.put(account, 0);
                CreateOp createOp = new CreateOp(account);
                ledger.add(createOp);

                if (crossServerService == null)
                    this.setCrossServerService();
                if (!crossServerService.propagateState(createOp)) {
                    // If propagation fails, we need to revert the state
                    debug("NOK: propagation failed, reverting state...");
                    ledger.remove(createOp);
                    accounts.remove(account);
                }
            }
            debug("OK");
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void deleteAccount(String account) throws InactiveServerException, IsBrokerException, NonExistingAccountException, MoneyInAccountException {
        debug("> Deleting account " + account + "...");
        if (!isPrimaryServer()) {
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
                throw new InactiveServerException(this.role);
            }
            if (account.equals("broker")) {
                debug("NOK: " + account + " is the broker account");
                throw new IsBrokerException();
            }
            synchronized (accounts) {
                if (!accounts.containsKey(account)) {
                    debug("NOK: " + account + " does not exist");
                    throw new NonExistingAccountException(account);
                }
                int balance = accounts.get(account);
                if (balance > 0) {
                    debug("NOK: " + account + " still has money");
                    throw new MoneyInAccountException(account);
                }
                accounts.remove(account);
                DeleteOp deleteOp = new DeleteOp(account);
                ledger.add(deleteOp);

                if (crossServerService == null)
                    this.setCrossServerService();
                if (!crossServerService.propagateState(deleteOp)) {
                    // If propagation fails, we need to revert the state
                    debug("NOK: propagation failed, reverting state...");
                    ledger.remove(deleteOp);
                    accounts.put(account, balance);
                }
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

    public int getBalance(String account) throws InactiveServerException, NonExistingAccountException {
        debug("> Getting balance of account " + account + "...");
        int balance;
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            synchronized (accounts) {
                if (!accounts.containsKey(account)) {
                    debug("NOK: " + account + " does not exist");
                    throw new NonExistingAccountException(account);
                }
                balance = accounts.get(account);
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK: " + balance);
        return balance;
    }

    public void transferTo(String accountFrom, String accountTo, int amount) throws InactiveServerException, SameAccountException, InvalidAmountException, NonExistingAccountException, NotEnoughMoneyException {
        debug("> Transferring " + amount + " from " + accountFrom + " to " + accountTo);
        if (!isPrimaryServer()) {
            accounts.put(accountFrom, accounts.get(accountFrom) - amount);
            accounts.put(accountTo, accounts.get(accountTo) + amount);
            ledger.add(new TransferOp(accountFrom, accountTo, amount));
            return;
        }
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            if (accountFrom.equals(accountTo)) {
                debug("NOK: " + accountFrom + " and " + accountTo + " are the same account");
                throw new SameAccountException(accountFrom);
            }
            if (amount <= 0) {
                debug("NOK: " + amount + " is not a valid amount");
                throw new InvalidAmountException(Integer.toString(amount));
            }
            synchronized (accounts) {
                if (!accounts.containsKey(accountFrom)) {
                    debug("NOK: " + accountFrom + " does not exist");
                    throw new NonExistingAccountException(accountFrom);
                }
                if (!accounts.containsKey(accountTo)) {
                    debug("NOK: " + accountTo + " does not exist");
                    throw new NonExistingAccountException(accountTo);
                }
                if (accounts.get(accountFrom) < amount) {
                    debug("NOK: " + accountFrom + " does not have enough money");
                    throw new NotEnoughMoneyException(accountFrom);
                }
                accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                accounts.put(accountTo, accounts.get(accountTo) + amount);
                TransferOp transferOp = new TransferOp(accountFrom, accountTo, amount);
                ledger.add(transferOp);

                if (crossServerService == null)
                    this.setCrossServerService();
                if (!crossServerService.propagateState(transferOp)) {
                    // If propagation fails, we need to revert the state
                    debug("NOK: propagation failed, reverting state...");
                    ledger.remove(transferOp);
                    accounts.put(accountFrom, accounts.get(accountFrom) + amount);
                    accounts.put(accountTo, accounts.get(accountTo) - amount);
                }
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

}
