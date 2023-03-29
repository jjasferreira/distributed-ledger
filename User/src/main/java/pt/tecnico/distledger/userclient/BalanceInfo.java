package pt.tecnico.distledger.userclient;

import java.util.List;


public class BalanceInfo {

    private int value;

    private List<Integer> valueTS;

    public BalanceInfo(int value, List<Integer> valueTS) {
        this.value = value;
        this.valueTS = List<Integer>(valueTS);
    }

    public int getValue() {
        return this.value;
    }

    public List<Integer> getValueTS() {
        return this.valueTS;
    }

}
