package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.vectorclock.VectorClock;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;


// This class is used to store the operations in a sorted order
// If i<j then operations[i] happened before operations[j] or they are concurrent
// There cannot be two of the same operation
public class Ledger {

    private List<Operation> stableOps;

    private List<Operation> unstableOps;

    private List<Operation> allOps;

    private HashSet<VectorClock> existingOps;

    private boolean isStable;

    public Ledger() {
        stableOps = new ArrayList<>();
        unstableOps = new ArrayList<>();
        allOps = new ArrayList<>();
        existingOps = new HashSet<>();
    }

    public List<Operation> getStableOps() {
        return this.stableOps;
    }

    public List<Operation> getUnstableOps() {
        return this.unstableOps;
    }

    public List<Operation> getAllOps() {
        return this.allOps;
    }

    public void insert(Operation op, boolean isStable) {
        if (existingOps.contains(op.getUpdateTS()))
            return;
        existingOps.add(op.getUpdateTS());
        allOps.add(op);
        List<Operation> ops = isStable ? stableOps : unstableOps;
        int insertIndex = ops.size();
        for (int i = 0; i < insertIndex; i++) {
            Operation prevOp = ops.get(i);
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
        ops.add(insertIndex, op);
    }


    public void stabilize(int index) {
        stableOps.add(unstableOps.get(index));
        unstableOps.remove(index);
    }

    @Override
    public String toString() {
        // Return a string of the form {op1},{op2},...,{opn}
        StringBuilder sb = new StringBuilder();
        int ledgerSize = allOps.size();
        for (int i = 0; i < ledgerSize; i++) {
            sb.append("{");
            sb.append(allOps.get(i).toString());
            if (i < ledgerSize - 1)
                sb.append("},\n");
            else
                sb.append("}");
        }
        return sb.toString();
    }

}
