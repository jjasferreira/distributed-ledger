package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.*;

import java.util.List;

public class LedgerState {

    private final List<Operation> ledger;

    public LedgerState(List<Operation> ledger) {
        this.ledger = ledger;
    }

    public String toString() {
        return "TOSTRING TO BE IMPLEMENTED";
    }
}
