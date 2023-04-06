package pt.tecnico.distledger.server.domain.operation;

import java.util.ArrayList;
import java.util.List;

// import vector clock
import pt.tecnico.distledger.server.vectorclock.VectorClock;

public class UpdateLog {
    // This class is used to store the operations in a sorted order. If i<j then operations[i] happened before operations[j] or they are concurrent.
    private List<Operation> operations;

    private boolean isStable;

    public UpdateLog(boolean isStable) {
        operations = new ArrayList<>();
        this.isStable = isStable;
    }

    public void insert(Operation op) {
        int insertIndex = operations.size();
        for (int i = 0; i < operations.size(); i++) {
            Operation prevOp = operations.get(i);
            if (op.getVectorClock().isConcurrent(prevOp.getVectorClock())) {
                continue;
            } else if (op.getVectorClock().happensBefore(prevOp.getVectorClock())) {
                insertIndex = i;
                break;
            } else {
                insertIndex = i + 1;
            }
        }
        operations.add(insertIndex, op);
    }

    public List<Operation> getOperations() {
        return operations;
    }
}
