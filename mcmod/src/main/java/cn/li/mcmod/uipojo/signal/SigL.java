package cn.li.mcmod.uipojo.signal;

import java.util.ArrayList;

public final class SigL implements ISigL, IDoubleSource, ISupportsOuts {

    private long value;
    private final ArrayList<IDep> outs;

    public SigL(long value, ArrayList<IDep> outs) {
        this.value = value;
        this.outs = outs;
    }

    @Override
    public long lGet() {
        return value;
    }

    @Override
    public void lSet(long v) {
        if (value != v) {
            value = v;
            SignalSupport.notifyOuts(outs);
        }
    }

    @Override
    public double readAsDouble() {
        return (double) value;
    }

    @Override
    public ArrayList<IDep> getOuts() {
        return outs;
    }
}
