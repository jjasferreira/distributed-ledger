package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;


public class TransferOp extends Operation {

    private String destAccount;

    private int amount;

    public TransferOp(String fromAccount, String destAccount, int amount, VectorClock prevTS, VectorClock updateTS, int replicaIndex) {
        super(fromAccount, prevTS, updateTS, replicaIndex);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public String getDestAccount() {
        return destAccount;
    }

    public void setDestAccount(String destAccount) {
        this.destAccount = destAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "TransferOp: from=" + getAccount()
                + ", to=" + this.destAccount
                + ", amount=" + this.amount
                + ", prevTS=" + getPrevTS()
                + ", updateTS=" + getUpdateTS()
                + ", replicaIndex=" + getReplicaIndex();
    }

}
