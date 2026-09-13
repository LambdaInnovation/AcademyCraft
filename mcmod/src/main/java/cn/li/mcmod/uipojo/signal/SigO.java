package cn.li.mcmod.uipojo.signal;

import clojure.lang.RT;
import java.util.ArrayList;

public final class SigO implements ISigO, IDoubleSource, ISupportsOuts {

    private Object value;
    private final ArrayList<IDep> outs;

    public SigO(Object value, ArrayList<IDep> outs) {
        this.value = value;
        this.outs = outs;
    }

    @Override
    public Object sGet() {
        return value;
    }

    @Override
    public void sSet(Object v) {
        if (!SignalSupport.objectEquals(value, v)) {
            value = v;
            SignalSupport.notifyOuts(outs);
        }
    }

    @Override
    public double readAsDouble() {
        return RT.doubleCast(value);
    }

    @Override
    public ArrayList<IDep> getOuts() {
        return outs;
    }
}
