package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

// import vector clock


public class Ledger {
    // This class is used to store the operations in a sorted order. If i<j then operations[i] happened before operations[j] or they are concurrent. There cannot be two of the same operation
    private List<Operation> stableOperations;

    private List<Operation> unstableOperations;

    private List<Operation> everyOperation;

    private HashSet<VectorClock> existingOperations;

    private boolean isStable;

    public Ledger() {
        stableOperations = new ArrayList<>();
        unstableOperations = new ArrayList<>();
        everyOperation = new ArrayList<>();
        existingOperations = new HashSet<>();
    }


    public void insert(Operation op, boolean isStable) {
        if (existingOperations.contains(op.getUpdateTS())) {
            return;
        }
        existingOperations.add(op.getUpdateTS());
        everyOperation.add(op);
        List<Operation> operations = isStable ? stableOperations : unstableOperations;
        int insertIndex = operations.size();
        for (int i = 0; i < operations.size(); i++) {
            Operation prevOp = operations.get(i);
            // TODO: is this ordered based on prevTS or based on updateTS?
            if (op.getPrevTS().isConcurrent(prevOp.getPrevTS())) {
                continue;
            } else if (op.getPrevTS().happensBefore(prevOp.getPrevTS())) {
                insertIndex = i;
                break;
            } else {
                insertIndex = i + 1;
            }
        }
        operations.add(insertIndex, op);
    }

    public List<Operation> getStableOperations() {
        return stableOperations;
    }

    public List<Operation> getUnstableOperations() {
        return unstableOperations;
    }

    public List<Operation> getEveryOperation() {
        return everyOperation;
    }

    /*
    public void remove(int index, boolean isStable) {
        if (isStable)
            stableOperations.remove(index);
        else
            unstableOperations.remove(index);
    }
    */

    public void stabilize(int index) {
        stableOperations.add(unstableOperations.get(index));
        unstableOperations.remove(index);
    }

    @Override
    public String toString() {
        /*returns a String of the form {op1};{op2};...{opn}; */
        StringBuilder sb = new StringBuilder();
        for (Operation op : everyOperation) {
            sb.append("{");
            sb.append(op.toString());
            sb.append("};\n");
        }
        return sb.toString();
    }
}