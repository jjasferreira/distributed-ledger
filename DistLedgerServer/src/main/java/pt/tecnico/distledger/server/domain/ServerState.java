package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.grpc.CrossServerService;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.vectorclock.VectorClock;
import pt.tecnico.distledger.server.domain.operation.UpdateLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.locks.*;


public class ServerState {

    private static final int LIST_SIZE = 3;

    // private final List<Operation> ledger;

    private final HashMap<String, Integer> accounts;

    private final String serviceName;

    private final String role;

    private final String address;

    private boolean active;

    private final boolean debug;

    private final NamingServerService namingServerService;

    // table containing the replica's own timestamp and the timestamps for the other servers as per the last gossip message
    private HashMap<String, VectorClock> timestampTable;

    // timestamp of the last value written to the ledger
    private VectorClock valueTimestamp;

    private HashMap<String, CrossServerService> crossServerServices;

    // Map from role to replica index in the timestamps
    private HashMap<String, Integer> roleIndexes;

    // Stable operation update log
    private UpdateLog ledger;

    // Unstable operation update log
    private UpdateLog unstableLedger;

    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug, NamingServerService namingServerService, String serviceName, String role, String address) {
        this.debug = debug;
        this.namingServerService = namingServerService;
        this.serviceName = serviceName;
        this.role = role;
        this.address = address;
        this.active = true;
        // this.ledger = new ArrayList<>();
        this.unstableLedger = new UpdateLog(false);
        this.ledger = new UpdateLog(true);
        this.accounts = new HashMap<>();
        this.setCrossServerServices();
        this.timestampTable = new HashMap<>();
        this.timestampTable.put(this.role, new VectorClock(new ArrayList<>(Collections.nCopies(LIST_SIZE, 0))));
        this.valueTimestamp = new VectorClock(new ArrayList<>(Collections.nCopies(LIST_SIZE, 0)));
        this.roleIndexes = new HashMap<>();
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
                // TODO how to return both active and inactive operations
                return new ArrayList<>(ledger.getOperations());
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public VectorClock createAccount(String account, VectorClock prevTS) throws InactiveServerException, AlreadyExistingAccountException {
        debug("> Creating account " + account + "...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            synchronized (accounts) {
                // increment replica timestamp
                int replicaIndex = this.roleIndexes.get(this.role);
                this.timestampTable.get(this.role).increment(replicaIndex);
                VectorClock updateTS = new VectorClock(prevTS.toList());
                // updateTS is equal to prevTS, with the value in the replica index set to the same as in the replica timestamp
                updateTS.setIndex(replicaIndex, this.timestampTable.get(this.role).getIndex(replicaIndex));
                //debug everything
                CreateOp createOp = new CreateOp(account, prevTS, updateTS, replicaIndex);
                debug("prevTS: " + prevTS + " updateTS: " + updateTS + " valueTS: " + this.valueTimestamp + " replicaTS: " + this.timestampTable.get(this.role).toString());
                // if prevTS <= valueTS , then the value is valid
                if (prevTS.happensBefore(this.valueTimestamp) || prevTS.isEqual(this.valueTimestamp)) {
                    if (accounts.containsKey(account)) {
                        debug("NOK: " + account + " already exists");
                        throw new AlreadyExistingAccountException(account);
                    }
                    debug("Creating account " + account + "...");
                    accounts.put(account, 0);
                    ledger.insert(createOp);
                    this.valueTimestamp.increment(replicaIndex);
                    // display new valuetimestamp
                    debug("New valueTS: " + this.valueTimestamp);
                } else {
                    debug("Adding operation to unstable ledger...");
                    unstableLedger.insert(createOp);
                }
                debug("OK");
                return updateTS;
            }
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
            // if prevTS <= valueTS , then the value is valid
            if (prevTS.happensBefore(this.valueTimestamp) || prevTS.isEqual(this.valueTimestamp)) {
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

    public VectorClock transferTo(String accountFrom, String accountTo, int amount, VectorClock prevTS) throws InactiveServerException, SameAccountException, InvalidAmountException, NonExistingAccountException, NotEnoughMoneyException {
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
                // increment replica timestamp
                int replicaIndex = this.roleIndexes.get(this.role);
                this.timestampTable.get(this.role).increment(replicaIndex);
                VectorClock updateTS = new VectorClock(prevTS.toList());
                // updateTS is equal to prevTS, with the value in the replica index set to the same as in the replica timestamp
                updateTS.setIndex(replicaIndex, this.timestampTable.get(this.role).getIndex(replicaIndex));
                TransferOp transferOp = new TransferOp(accountFrom, accountTo, amount, prevTS, updateTS, replicaIndex);
                //debug everything
                debug("prevTS: " + prevTS + " updateTS: " + updateTS + " valueTS: " + this.valueTimestamp + " replicaTS: " + this.timestampTable.get(this.role).toString());

                // if prevTS <= valueTS , then the value is valid
                if (prevTS.happensBefore(this.valueTimestamp) || prevTS.isEqual(this.valueTimestamp)) {
                    accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                    accounts.put(accountTo, accounts.get(accountTo) + amount);
                    ledger.insert(transferOp);
                } else {
                    unstableLedger.insert(transferOp);
                }
                debug("OK");
                return updateTS;
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void receivePropagatedState(Operation op) throws InactiveServerException, UnknownOperationException {
        /*
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

        */
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
