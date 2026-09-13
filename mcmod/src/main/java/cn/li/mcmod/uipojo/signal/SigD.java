package cn.li.mcmod.uipojo.signal;

import java.util.ArrayList;

public final class SigD implements ISigD, IDoubleSource, ISupportsOuts {

    private double value;
    private final ArrayList<IDep> outs;

    public SigD(double value, ArrayList<IDep> outs) {
        this.value = value;
        this.outs = outs;
    }

    @Override
    public double dGet() {
        return value;
    }

    @Override
    public void dSet(double v) {
        if (value != v) {
            value = v;
            SignalSupport.notifyOuts(outs);
        }
    }

    @Override
    public double readAsDouble() {
        return value;
    }

    @Override
    public ArrayList<IDep> getOuts() {
        return outs;
    }
}
