package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

import java.util.List;

public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        // deprecated; code is here so program compiles
        super(account, new VectorClock(List.of(0,0,0)), new VectorClock(List.of(0,0,0)), 0);
    }

}
