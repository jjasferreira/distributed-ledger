package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

public class Operation {
    private String account;

    private VectorClock prevTS;

    private VectorClock updateTS;

    private int replicaIndex;

    public Operation(String fromAccount, VectorClock prevTS, VectorClock updateTS, int replicaIndex) {
        this.account = fromAccount;
        this.prevTS = prevTS;
        this.updateTS = updateTS;
        this.replicaIndex = replicaIndex;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public VectorClock getPrevTS() {
        return new VectorClock(prevTS.getClock());
    }

    public VectorClock getUpdateTS() {
        return new VectorClock(updateTS.getClock());
    }

    public int getReplicaIndex() {
        return replicaIndex;
    }

}
