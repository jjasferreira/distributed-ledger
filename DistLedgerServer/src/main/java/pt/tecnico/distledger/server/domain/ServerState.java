package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.grpc.CrossServerService;

import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.vectorclock.VectorClock;
import pt.tecnico.distledger.server.domain.operation.Ledger;

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
    private Ledger ledger;


    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug, NamingServerService namingServerService, String serviceName, String role, String address) {
        this.debug = debug;
        this.namingServerService = namingServerService;
        this.serviceName = serviceName;
        this.role = role;
        this.address = address;
        this.active = true;
        this.ledger = new Ledger();
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
                return new ArrayList<>(ledger.getEveryOperation());
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
                    ledger.insert(createOp, true);
                    this.valueTimestamp.increment(replicaIndex);
                    // display new valuetimestamp
                    debug("New valueTS: " + this.valueTimestamp);
                } else {
                    debug("Adding operation to unstable ledger...");
                    ledger.insert(createOp, false);
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
                    ledger.insert(transferOp, true);
                } else {
                    ledger.insert(transferOp, false);
                }
                debug("OK");
                return updateTS;
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void receivePropagatedState(Ledger incomingLedger, VectorClock incomingReplicaTimestamp, String replicaRole) throws InactiveServerException {
        debug("> Receiving propagated state from server with role " + replicaRole + ": " + incomingLedger.toString());
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            int replicaIndex = this.roleIndexes.get(replicaRole);
            //updates replica table for the replica that sent the state
            this.timestampTable.put(replicaRole, incomingReplicaTimestamp);
            //merges replica timestamp with incoming replica timestamp
            this.timestampTable.get(this.role).mergeClocks(incomingReplicaTimestamp);

            // adds every incoming op
            for (Operation op : incomingLedger.getEveryOperation()) {
                ledger.insert(op, false);
            }

            this.checkUnstableLedger();
        }
        finally {
            activeLock.readLock().unlock();
        }
    }


    public void checkUnstableLedger() {
        // goes through unstable ledger and checks, for every operation, if its prevTS <= valueTS. If so, sets valueTS to the updateTS of the operation
        int index = 0;
        for (Operation op : ledger.getUnstableOperations()) {
            if (op.getPrevTS().happensBefore(this.valueTimestamp) || op.getPrevTS().isEqual(this.valueTimestamp)) {
                this.valueTimestamp.setClock(op.getUpdateTS());
                ledger.stabilize(index);
                index++;
                executeOperation(op);
            }
        }
    }

    public void executeOperation(Operation op) {
        // executes operation
        if (op instanceof TransferOp) {
            TransferOp transferOp = (TransferOp) op;
            synchronized (accounts) {
                String accountFrom = transferOp.getAccount();
                String accountTo = transferOp.getDestAccount();
                int amount = transferOp.getAmount();
                accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                accounts.put(accountTo, accounts.get(accountTo) + amount);
            }
        } else if (op instanceof CreateOp) {
            CreateOp createOp = (CreateOp) op;
            synchronized (accounts) {
                accounts.put(createOp.getAccount(), 0);
            }
        }
    }

    public void gossip(String role) { // TODO create new exceptions
        debug("> Gossiping with " + role + "...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                return;
            }
            // if role cannot be found, lookup
            if (!this.crossServerServices.containsKey(role)) {
                this.setCrossServerServices();
                if (!this.crossServerServices.containsKey(role)) {
                    debug("NOK: " + role + " not found");
                    // TODO throw exception
                    return;
                }
            }
            // get the cross server service
            CrossServerService crossServerService = this.crossServerServices.get(role);
            //get the other server's replicaTimestamp
            VectorClock otherReplicaTimestamp = timestampTable.get(role);
            //create temporary ledger (updateLog)
            Ledger tempLedger = new Ledger();
            // iterate through stable ledger operations
            for (Operation op : ledger.getStableOperations()) {
                // get the prevTS
                VectorClock prevTS = op.getPrevTS();
                //get the replicaIndex
                int replicaIndex = op.getReplicaIndex();
                // if the replicaIndex position of the replicaTimestamp is less than the replicaIndex position of the prevTS, add to the tempLedger
                // this means that to the knowledge of the present replica, the other replica has not yet received any updates after the one indicated by timestamp[replicaIndex]
                if (otherReplicaTimestamp.getIndex(replicaIndex) < prevTS.getIndex(replicaIndex)) {
                    tempLedger.insert(op, false);
                }
            }
            // iterate through unstable ledger operations
            for (Operation op : ledger.getUnstableOperations()) {
                VectorClock prevTS = op.getPrevTS();
                int replicaIndex = op.getReplicaIndex();
                if (otherReplicaTimestamp.getIndex(replicaIndex) < prevTS.getIndex(replicaIndex)) {
                    tempLedger.insert(op, false);
                }
            }
            if (!crossServerService.propagateState(tempLedger.getEveryOperation(), timestampTable.get(this.role).toList())) {
                debug("NOK: gossiping with " + role + " failed");
                // TODO throw exception
                return;
            }
        } finally {
            activeLock.readLock().unlock();
        }
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
