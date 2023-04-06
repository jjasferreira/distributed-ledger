package pt.tecnico.distledger.server.vectorclock;

public enum EventOrdering {
    CONCURRENT, HAPPENS_BEFORE, HAPPENS_AFTER, IDENTICAL, NOT_COMPARABLE;
}
