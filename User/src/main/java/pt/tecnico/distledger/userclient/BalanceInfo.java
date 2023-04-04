package pt.tecnico.distledger.userclient;

import java.util.List;
import java.util.stream.Collectors;


public class BalanceInfo {

    private int value;

    private List<Integer> valueTS;

    public BalanceInfo(int value, List<Integer> valueTS) {
        this.value = value;
        this.valueTS = valueTS.stream().collect(Collectors.toList());
    }

    public int getValue() {
        return this.value;
    }

    public List<Integer> getValueTS() {
        return this.valueTS;
    }

}
