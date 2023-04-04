package pt.tecnico.distledger.server.vectorclock;

import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;


public class VectorClock implements Comparable<VectorClock> {

    private List<Integer> clock;

    public VectorClock(List<Integer> clock) {
        this.clock = clock;
    }

    public List<Integer> getClock() {
        return clock;
    }

    public void setClock(List<Integer> clock) {
        this.clock = clock;
    }

    public void incrementClock(int index) {
        this.clock.set(index, this.clock.get(index) + 1);
    }

    public void mergeClocks(List<Integer> otherClock) {
        for (int i = 0; i < this.clock.size(); i++) {
            this.clock.set(i, Math.max(this.clock.get(i), otherClock.get(i)));
        }
    }

    public boolean isConcurrent(VectorClock other) {
        return this.compareTo(other) == 0;
    }

    @Override
    public int compareTo(VectorClock other) {
        // Returns 1 if this is greater than other, 0 if they are equal, -1 if other is greater than this
        List<Integer> otherClock = other.getClock();
        List<Integer> thisClock = this.getClock();


        //diff is a list of the differences between the clocks
        List<Integer> diff = thisClock.stream()
                .map(i -> i - otherClock.get(thisClock.indexOf(i)))
                .collect(Collectors.toList());

        return diff.stream()
                .max(Comparator.naturalOrder())
                .get();
    }

    public List toList() {
        //return copy of clock
        return this.clock.stream().collect(Collectors.toList());
    }
    @Override
    public String toString() {
        return clock.toString();
    }
}