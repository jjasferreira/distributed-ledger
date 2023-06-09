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

import static io.grpc.Status.UNAVAILABLE;


public class ServerState {

    private static final int LIST_SIZE = 3;

    private final String serviceName;

    private final String role;

    private final String address;

    private boolean active;

    private final boolean debug;

    private final NamingServerService namingServerService;

    private HashMap<String, CrossServerService> crossServerServices;

    // Map from account name to current balance
    private final HashMap<String, Integer> accounts;

    // Stable operation update log
    private Ledger ledger;

    // Timestamp of the last value written to the ledger
    private VectorClock valueTS;

    // Table containing the replica's own timestamp and the timestamps for the other servers as per the last gossip message
    private HashMap<String, VectorClock> timestampTable;

    // Map from role to replica index in the timestamps
    private HashMap<String, Integer> roleIndex;

    // ReadWriteLock is preferable to synchronized call because it allows for multiple readers
    private final ReadWriteLock activeLock = new ReentrantReadWriteLock();

    public ServerState(boolean debug, NamingServerService namingServerService, String serviceName, String role, String address) {
        this.debug = debug;
        this.namingServerService = namingServerService;
        this.serviceName = serviceName;
        this.role = role;
        this.address = address;
        this.active = true;
        this.timestampTable = new HashMap<>();
        this.timestampTable.put(this.role, new VectorClock(new ArrayList<>(Collections.nCopies(LIST_SIZE, 0))));
        this.crossServerServices = new HashMap<>();
        this.setCrossServerInfo();
        this.accounts = new HashMap<>();
        this.accounts.put("broker", 1000);
        this.ledger = new Ledger();
        this.valueTS = new VectorClock(new ArrayList<>(Collections.nCopies(LIST_SIZE, 0)));
        this.roleIndex = new HashMap<>();
    }

    public List<Integer> getValueTS() {
        return this.valueTS.toList();
    }

    public boolean registerToNamingServer(String name, String role, String address) {
        debug("> Registering server to naming server...");
        try {
            int replicaIndex = namingServerService.register(name, role, address);
            this.roleIndex.put(role, replicaIndex);
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

    private boolean setCrossServerInfo() {
        // Add all available servers to crossServerServices
        HashMap<String, String> servers = namingServerService.lookup(this.serviceName, "");
        if (servers.isEmpty()) {
            debug("OK: no other servers found");
            return false;
        }
        debug("OK, found servers with addresses: " + servers.keySet());
        for (HashMap.Entry<String, String> server : servers.entrySet()) {
            String role = server.getValue();
            if (role.equals(this.role))
                continue;
            this.crossServerServices.put(role, new CrossServerService(server.getKey()));
            // Add timestamp table entry
            if (!timestampTable.containsKey(role))
                this.timestampTable.put(role, new VectorClock(new ArrayList<>(Collections.nCopies(LIST_SIZE, 0))));
        }
        return true;
    }

    private void debug(String message) {
        if (this.debug)
            System.err.println("[DEBUG] " + message);
    }

    public void activate() throws AlreadyActiveServerException {
        debug("> Receiving request to activate server...");
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
        debug("> Receiving request to deactivate server...");
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

    public List<Operation> getUnstableLedger() {
        debug("> Receiving request to get unstable ledger...");
        synchronized (accounts) {
            debug("OK");
            return new ArrayList<>(ledger.getUnstableOps());
        }
    }

    public List<Operation> getStableLedger() {
        debug("> Receiving request to get stable ledger...");
        synchronized (accounts) {
            debug("OK");
            return new ArrayList<>(ledger.getStableOps());
        }
    }

    public List<Operation> getLedger() {
        debug("> Receiving request to get ledger...");
        synchronized (accounts) {
            debug("OK");
            return new ArrayList<>(ledger.getAllOps());
        }
    }

    public VectorClock createAccount(String account, VectorClock prevTS) throws InactiveServerException, AlreadyExistingAccountException {
        debug("> Receiving request to create account " + account + "...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            int replicaIndex = this.roleIndex.get(this.role);
            VectorClock updateTS = new VectorClock(prevTS.toList());
            synchronized (accounts) {
                // Increment replicaTS, set updateTS to be equal to prevTS except for the replica index
                VectorClock replicaTS = this.timestampTable.get(this.role);
                replicaTS.increment(replicaIndex);
                updateTS.setIndex(replicaIndex, replicaTS.getIndex(replicaIndex));
                CreateOp createOp = new CreateOp(account, prevTS, updateTS, replicaIndex);
                debug("valueTS: " + this.valueTS + ", prevTS: " + prevTS + ", updateTS: " + updateTS + ", replicaTS: " + replicaTS);
                // If prevTS <= valueTS , then the operation is stable
                if (prevTS.happensBefore(this.valueTS) || prevTS.isEqual(this.valueTS)) {
                    debug("Creating account...");
                    if (accounts.containsKey(account)) {
                        debug("NOK: " + account + " already exists (create account)");
                        replicaTS.decrement(replicaIndex);
                        throw new AlreadyExistingAccountException(account);
                    }
                    accounts.put(account, 0);
                    ledger.insert(createOp, true);
                    this.valueTS.increment(replicaIndex);
                } else {
                    debug("Adding unstable operation to ledger...");
                    ledger.insert(createOp, false);
                }
                debug("OK, valueTS: " + this.valueTS);
                return updateTS;
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public int getBalance(String account, VectorClock prevTS) throws InactiveServerException, NonExistingAccountException, InvalidTimestampException {
        debug("> Receiving request to get balance of account " + account + "...");
        int balance;
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            // If prevTS <= valueTS , then the value is valid
            if (prevTS.happensBefore(this.valueTS) || prevTS.isEqual(this.valueTS)) {
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
        debug("OK, balance: " + balance);
        return balance;
    }

    public VectorClock transferTo(String accountFrom, String accountTo, int amount, VectorClock prevTS) throws InactiveServerException, SameAccountException, InvalidAmountException, NonExistingAccountException, NotEnoughMoneyException {
        debug("> Receiving request to transfer " + amount + " from " + accountFrom + " to " + accountTo);
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
            int replicaIndex = this.roleIndex.get(this.role);
            VectorClock updateTS = new VectorClock(prevTS.toList());
            synchronized (accounts) {
                // Increment replicaTS, set updateTS to be equal to prevTS except for the replica index
                VectorClock replicaTS = this.timestampTable.get(this.role);
                replicaTS.increment(replicaIndex);
                updateTS.setIndex(replicaIndex, replicaTS.getIndex(replicaIndex));
                TransferOp transferOp = new TransferOp(accountFrom, accountTo, amount, prevTS, updateTS, replicaIndex);
                debug("valueTS: " + this.valueTS + ", prevTS: " + prevTS + ", updateTS: " + updateTS + ", replicaTS: " + replicaTS);
                // If prevTS <= valueTS , then the operation is stable
                if (prevTS.happensBefore(this.valueTS) || prevTS.isEqual(this.valueTS)) {
                    debug("Transferring money...");
                    if (!accounts.containsKey(accountFrom)) {
                        debug("NOK: " + accountFrom + " does not exist");
                        replicaTS.decrement(replicaIndex);
                        throw new NonExistingAccountException(accountFrom);
                    }
                    if (!accounts.containsKey(accountTo)) {
                        debug("NOK: " + accountTo + " does not exist");
                        replicaTS.decrement(replicaIndex);
                        throw new NonExistingAccountException(accountTo);
                    }
                    if (accounts.get(accountFrom) < amount) {
                        debug("NOK: " + accountFrom + " does not have enough money");
                        replicaTS.decrement(replicaIndex);
                        throw new NotEnoughMoneyException(accountFrom);
                    }
                    accounts.put(accountFrom, accounts.get(accountFrom) - amount);
                    accounts.put(accountTo, accounts.get(accountTo) + amount);
                    ledger.insert(transferOp, true);
                    this.valueTS.increment(replicaIndex);
                } else {
                    debug("Adding unstable operation to ledger...");
                    ledger.insert(transferOp, false);
                }
                debug("OK, valueTS: " + this.valueTS);
                return updateTS;
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void receivePropagatedState(Ledger incomingLedger, String replicaRole, VectorClock incomingReplicaTS) throws InactiveServerException {
        debug("> Receiving propagated state from server with role " + replicaRole + ": " + incomingLedger);
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            synchronized (accounts) {
                // Update timestamp table for the replica that sent the state
                this.timestampTable.put(replicaRole, incomingReplicaTS);
                // Merge replicaTS with incoming timestamp
                this.timestampTable.get(this.role).mergeClocks(incomingReplicaTS);
                // Add every incoming operation to the ledger and stabilize where possible
                for (Operation op : incomingLedger.getAllOps()) {
                    if (!ledger.contains(op.getUpdateTS()))
                        ledger.insert(op, false);
                }
                this.checkUnstableLedger();
            }
            debug("OK, valueTS: " + this.valueTS);
        }
        finally {
            activeLock.readLock().unlock();
        }
    }

    public void checkUnstableLedger() {
        // Go through unstable ledger and check, for every operation, if its prevTS <= valueTS
        // If so, set valueTS to the updateTS of the operation
        int index = 0;
        List<Integer> toDelete = new ArrayList<Integer>();
        for (Operation op : ledger.getUnstableOps()) {
            if (op.getPrevTS().happensBefore(this.valueTS) || op.getPrevTS().isEqual(this.valueTS)) {
                try {
                    executeOperation(op);
                } catch (AlreadyExistingAccountException | NonExistingAccountException | NotEnoughMoneyException e) {
                    debug(e.getMessage());
                }
                // cannot delete while iterating
                toDelete.add(index);
                this.valueTS.setClock(op.getUpdateTS());
                debug("OK, valueTS: " + this.valueTS);
                index++;
            }
        }
        // Must be in reverse order to avoid index out of bounds
        int toDeleteSize = toDelete.size();
        for (int i = toDeleteSize - 1; i >= 0; i--)
            ledger.stabilize(toDelete.get(i));
    }

    public void executeOperation(Operation op) throws AlreadyExistingAccountException, NonExistingAccountException, NotEnoughMoneyException {
        if (op instanceof CreateOp) {
            CreateOp createOp = (CreateOp) op;
            String account = createOp.getAccount();
            debug("> Creating account " + account);
            synchronized (accounts) {
                if (accounts.containsKey(account)) {
                    debug("NOK: accounts:" + accounts);
                    debug("NOK: " + account + " already exists (execute operation)");
                    throw new AlreadyExistingAccountException(account);
                }
                accounts.put(account, 0);
            }
        } else if (op instanceof TransferOp) {
            TransferOp transferOp = (TransferOp) op;
            String accountFrom = transferOp.getAccount();
            String accountTo = transferOp.getDestAccount();
            int amount = transferOp.getAmount();
            debug("> Transferring " + amount + " from " + accountFrom + " to " + accountTo);
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
            }
        }
    }

    public void gossip() throws InactiveServerException, NoServersFoundException, GossipFailedException {
        debug("> Receiving request to gossip...");
        activeLock.readLock().lock();
        try {
            if (!active) {
                debug("NOK: inactive server");
                throw new InactiveServerException(this.role);
            }
            if (!this.setCrossServerInfo()) {
                debug("NOK: no servers found to gossip with");
                throw new NoServersFoundException();
            }
            // For every server in cross server services
            for (String role : this.crossServerServices.keySet()) {
                // Get the cross server service and the other server's replicaTS
                CrossServerService crossServerService = this.crossServerServices.get(role);
                VectorClock otherReplicaTS = timestampTable.get(role);
                Ledger tempLedger = new Ledger();
                // Iterate through all our operations, adding them to the tempLedger if we estimate that the replica has not yet seen them
                for (Operation op : ledger.getAllOps()) {
                    VectorClock updateTS = op.getUpdateTS();
                    int replicaIndex = op.getReplicaIndex();
                    // If the replicaIndex position of the replicaTS is less than the replicaIndex position of the updateTS, add to the tempLedger
                    // This means that to the knowledge of the present replica, the other has not yet received any updates after the one indicated by replicaTS[replicaIndex]
                    if (otherReplicaTS.getIndex(replicaIndex) < updateTS.getIndex(replicaIndex))
                        tempLedger.insert(op, false);
                    //debug templedger
                }
                debug("Sending propagated state:" + tempLedger.toString());
                if (!crossServerService.propagateState(tempLedger.getAllOps(), this.role, timestampTable.get(this.role).toList())) {
                    debug("NOK: gossiping with " + role + " failed");
                    // Remove the cross server service if it is not responding
                    this.crossServerServices.remove(role);
                    throw new GossipFailedException(role);
                }
                debug("OK");
            }
        } finally {
            activeLock.readLock().unlock();
        }
    }

    public void shutdownServices() {
        debug("> Shutting down services...");
        namingServerService.shutdownNow();
        for (CrossServerService crossServerService : crossServerServices.values()) {
            if (crossServerService != null)
                crossServerService.shutdownNow();
        }
        debug("OK");
    }

}
