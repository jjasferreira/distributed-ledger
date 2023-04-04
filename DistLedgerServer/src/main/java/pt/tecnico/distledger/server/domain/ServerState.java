package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.grpc.CrossServerService;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.vectorclock.VectorClock;

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

    // table containing the replica's own timestamp and the timestamps for the other servers as per the last gossip message
    private HashMap<String, VectorClock> timestampTable;

    private VectorClock valueTimestamp;

    private HashMap<String, CrossServerService> crossServerServices;

    private HashMap<String, Integer> roleIndexes;

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
        this.setCrossServerServices();
        this.timestampTable = new HashMap<>();
        this.timestampTable.put(this.role, new VectorClock(List.of(0,0,0)));
        this.valueTimestamp = new VectorClock(List.of(0,0,0));
        accounts.put("broker", 1000);

    }

    public boolean registerToNamingServer(String name, String role, String address) {
        debug("> Registering server to naming server...");
        try {
            int replicaIndex = namingServerService.register(name, role, address);
            this.roleIndexes.put(role, replicaIndex);
            debug("OK");
            System.out.println("Server registered to naming server");
            return true;
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            return false;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public void deleteFromNamingServer(String name, String address) {
        debug("> Deleting server from naming server...");
        try {
            namingServerService.delete(name, address);
            debug("OK");
            System.out.println("Server deleted from naming server");
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private boolean setCrossServerServices() {
        // TODO: change this function
        // Returns true if secondary server is found
        HashMap<String, String> servers = namingServerService.lookup(serviceName, "");
        if (servers.isEmpty())
            return false;
        // adds all servers to crossServerServices
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            this.crossServerServices.put(entry.getKey(), new CrossServerService(entry.getKey()));
        }
        return true;
    }

    private void debug(String message) {
        if (this.debug)
            System.err.println("[DEBUG] " + message);
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

    public List<Operation> getLedger() throws InactiveServerException {
        debug("> Getting ledger state...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            synchronized (accounts) {
                debug("OK");
                return new ArrayList<>(ledger);
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void createAccount(String account) throws InactiveServerException, AlreadyExistingAccountException {
        debug("> Creating account " + account + "...");
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
            }
            debug("OK");
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public int getBalance(String account, VectorClock prevTS) throws InactiveServerException, NonExistingAccountException, InvalidTimestampException {
        debug("> Getting balance of account " + account + "...");
        int balance;
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            // if valueTS >= prevTS, then the value is valid
            if (this.valueTimestamp.compareTo(prevTS) >= 0) {
                synchronized (accounts) {
                    if (!accounts.containsKey(account)) {
                        debug("NOK: " + account + " does not exist");
                        throw new NonExistingAccountException(account);
                    }
                    balance = accounts.get(account);
                }
            } else {
                throw new InvalidTimestampException(prevTS.toString());
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK: " + balance);
        return balance;
    }

    public void transferTo(String accountFrom, String accountTo, int amount) throws InactiveServerException, SameAccountException, InvalidAmountException, NonExistingAccountException, NotEnoughMoneyException {
        debug("> Transferring " + amount + " from " + accountFrom + " to " + accountTo);
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
            }
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

    public void receivePropagatedState(Operation op) throws InactiveServerException, UnknownOperationException {
        debug("> Receiving propagated state: " + op);
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            if (op instanceof CreateOp) {
                String account = op.getAccount();
                synchronized (accounts) {
                    accounts.put(account, 0);
                    ledger.add(new CreateOp(account));
                }
            } else if (op instanceof TransferOp) {
                TransferOp transferOp = (TransferOp) op;
                String accountFrom = transferOp.getAccount();
                String accountTo = transferOp.getDestAccount();
                int amount = transferOp.getAmount();
                synchronized (accounts) {
                    accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                    accounts.put(accountTo, accounts.get(accountTo) + amount);
                    ledger.add(new TransferOp(accountFrom, accountTo, amount));
                }
            } else if (op instanceof DeleteOp) {
                String account = op.getAccount();
                synchronized (accounts) {
                    accounts.remove(account);
                    ledger.add(new DeleteOp(account));
                }
            } else
                throw new UnknownOperationException();
        } finally {
            activeLock.readLock().unlock();
        }
        debug("OK");
    }

    public void gossip() {
        // TODO: implement gossip command
    }

    public void shutdownServices() {
        debug("> Shutting down services...");
        namingServerService.shutdownNow();
        // TODO: change to delete all cross server services
        for (CrossServerService crossServerService : crossServerServices.values())
            if (crossServerService != null)
                crossServerService.shutdownNow();
        debug("OK");
    }

    public List<Integer> getValueTimestamp() {
        return this.valueTimestamp.toList();
    }

}
