package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

import java.util.ArrayList;
import java.util.Vector;

public class Operation {
    private String account;

    private VectorClock vectorClock;

    public Operation(String fromAccount, VectorClock vectorClock) {
        this.account = fromAccount;
        this.vectorClock = vectorClock;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public VectorClock getVectorClock() {
        return new VectorClock(vectorClock.getClock());
    }

}
