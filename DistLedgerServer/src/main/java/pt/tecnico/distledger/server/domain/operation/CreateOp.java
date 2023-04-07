package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

public class CreateOp extends Operation {

    public CreateOp(String account, VectorClock prevTS, VectorClock updateTS, int replicaIndex) {
        super(account, prevTS, updateTS, replicaIndex);
    }

}
