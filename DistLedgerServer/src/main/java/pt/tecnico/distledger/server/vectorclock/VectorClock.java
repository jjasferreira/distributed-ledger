package pt.tecnico.distledger.server.vectorclock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class VectorClock {

    private List<Integer> clock;

    public VectorClock(List<Integer> clock) {
        this.clock = clock;
    }

    public List<Integer> getClock() {
        return clock;
    }

    public void setIndex(int index, int value) {
        this.clock.set(index, value);
    }

    public int getIndex(int index) {
        return this.clock.get(index);
    }

    public void increment(int index) {
        this.clock.set(index, this.clock.get(index) + 1);
    }

    public void decrement(int index) {
        this.clock.set(index, this.clock.get(index) - 1);
    }

    public void mergeClocks(VectorClock otherClock) {
        List<Integer> otherClockList = otherClock.toList();
        for (int i = 0; i < this.clock.size(); i++)
            this.clock.set(i, Math.max(this.clock.get(i), otherClockList.get(i)));
    }

    public boolean happensBefore(VectorClock other) {
        return this.compareClocks(other) == EventOrdering.HAPPENS_BEFORE;
    }

    public boolean isConcurrent(VectorClock other) {
        return this.compareClocks(other) == EventOrdering.CONCURRENT;
    }

    public boolean isEqual(VectorClock other) {
        return this.compareClocks(other) == EventOrdering.IDENTICAL;
    }

    /**
   * Compare two vector clocks and return:<br/>
   *
   * 1. IDENTICAL if the count and values all match<br/>
   * 2. HAPPENS_BEFORE if all stamps of clockOne happen before those of clockTwo<br/>
   * 3. HAPPENS_AFTER if all stamps of clockOne happen after those of clockTwo<br/>
   * 4. CONCURRENT if some stamps of clockOne and clockTwo are reverse ordered<br/>
   * 5. NOT_COMPARABLE otherwise
   */
    public EventOrdering compareClocks(VectorClock other) {
        List<Integer> thisClock = this.getClock();
        List<Integer> otherClock = other.getClock();

        // If the sizes do not match, the clocks are not comparable
        if (thisClock.size() != otherClock.size())
            return EventOrdering.NOT_COMPARABLE;

        // Check if the clocks are identical
        if (thisClock.equals(otherClock))
            return EventOrdering.IDENTICAL;

        // Check if the clocks are ordered
        boolean happensBefore = true;
        boolean happensAfter = true;
        // if there is an element that is before another element, and another that comes after another element, the clocks are concurrent
        boolean elementBefore = false;
        boolean elementAfter = false;

        for (int i = 0; i < thisClock.size(); i++) {
            if (thisClock.get(i) < otherClock.get(i)) {
                happensAfter = false;
                elementBefore = true; // There is an element that is before another element
            } else if (thisClock.get(i) > otherClock.get(i)) {
                happensBefore = false;
                elementAfter = true; // There is an element that is after another element
            }
        }
        if (elementBefore && elementAfter)
            return EventOrdering.CONCURRENT;
        else if (happensBefore)
            return EventOrdering.HAPPENS_BEFORE;
        else if (happensAfter)
            return EventOrdering.HAPPENS_AFTER;
        return EventOrdering.NOT_COMPARABLE;
    }

    public List<Integer> toList() {
        // Return copy of clock
        return this.clock.stream().collect(Collectors.toList());
    }

    public void setClock(VectorClock clock) {
        // Set this clock to a copy of the given clock
        this.clock = new ArrayList<>(clock.toList());
    }

    @Override
    public String toString() {
        return clock.toString();
    }

}
