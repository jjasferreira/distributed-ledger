package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.tecnico.distledger.server.domain.exception.*;
import pt.tecnico.distledger.server.grpc.NamingServerService;
import pt.tecnico.distledger.server.grpc.CrossServerService;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType;

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

    private boolean setCrossServerService() {
        // Returns true if secondary server is found
        HashMap<String, String> servers = namingServerService.lookup(serviceName, "B");
        if (servers.isEmpty()) {
            return false;
        }
        for (HashMap.Entry<String, String> entry : servers.entrySet()) {
            if (entry.getValue().equals("B")) {
                this.crossServerService = new CrossServerService(entry.getKey());
            }
        }
        return true;
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

    public void createAccount(String account) throws WrongServerRoleException, InactiveServerException, AlreadyExistingAccountException, NoSecondaryServerException {
        debug("> Creating account " + account + "...");
        if (!isPrimaryServer())
            throw new WrongServerRoleException(this.role);
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
                // If there is no secondary server service and none can be found
                if (crossServerService == null && !this.setCrossServerService())
                    throw new NoSecondaryServerException();
                // If propagation fails, we need to revert the state
                if (!crossServerService.propagateState(createOp)) {
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

    public void deleteAccount(String account) throws WrongServerRoleException, InactiveServerException, IsBrokerException, NonExistingAccountException, MoneyInAccountException, NoSecondaryServerException {
        debug("> Deleting account " + account + "...");
        if (!isPrimaryServer())
            throw new WrongServerRoleException(this.role);
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
                // If there is no secondary server service and none can be found
                if (crossServerService == null && !this.setCrossServerService())
                    throw new NoSecondaryServerException();
                // If propagation fails, we need to revert the state
                if (!crossServerService.propagateState(deleteOp)) {
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
        activeLock.readLock().lock();
        int balance;
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

    public void transferTo(String accountFrom, String accountTo, int amount) throws WrongServerRoleException, InactiveServerException, SameAccountException, InvalidAmountException, NonExistingAccountException, NotEnoughMoneyException, NoSecondaryServerException {
        debug("> Transferring " + amount + " from " + accountFrom + " to " + accountTo);
        if (!isPrimaryServer())
            throw new WrongServerRoleException(this.role);
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
                // If there is no secondary server service and none can be found
                if (crossServerService == null && !this.setCrossServerService())
                    throw new NoSecondaryServerException();
                // If propagation fails, we need to revert the state
                if (!crossServerService.propagateState(transferOp)) {
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

    public void receivePropagatedState(Operation op) throws InactiveServerException, WrongServerRoleException, UnknownOperationException {
        debug("> Receiving propagated state: " + op);
        if (isPrimaryServer())
            throw new WrongServerRoleException(this.role);
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

}
