package cn.li.mcmod.uipojo.signal;

import clojure.lang.Cons;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import java.util.ArrayList;

public final class ComputedD implements ISigD, IDoubleSource, IDep, ISupportsOuts {

    private final IDoubleSource s0;
    private final IDoubleSource s1;
    private final IDoubleSource s2;
    private final IDoubleSource[] moreSources;
    private final IFn f;
    private final int nSources;
    private double value;
    private boolean dirty;
    private final ArrayList<IDep> outs;

    public ComputedD(IDoubleSource s0,
                     IDoubleSource s1,
                     IDoubleSource s2,
                     IDoubleSource[] moreSources,
                     IFn f,
                     int nSources,
                     double value,
                     boolean dirty,
                     ArrayList<IDep> outs) {
        this.s0 = s0;
        this.s1 = s1;
        this.s2 = s2;
        this.moreSources = moreSources;
        this.f = f;
        this.nSources = nSources;
        this.value = value;
        this.dirty = dirty;
        this.outs = outs;
    }

    @Override
    public void depMarkDirty() {
        // Intentionally NOT guarded by `if (!dirty)`. Computeds are created
        // dirty=true and only clear dirty on read (dGet). A bound-but-never-read
        // computed would, under that guard, drop its FIRST source-change
        // notification and thus never propagate to its bindings again — freezing
        // clock-driven bindings (animations/ticks). Re-notifying while already
        // dirty is safe: Binding.depMarkDirty is idempotent via its `queued` flag,
        // and for the common single-source-per-frame case dGet clears dirty each
        // frame so no extra notifications occur anyway.
        dirty = true;
        SignalSupport.notifyOuts(outs);
    }

    @Override
    public double dGet() {
        if (dirty) {
            dirty = false;
            value = recompute();
        }
        return value;
    }

    @Override
    public void dSet(double v) {
        throw new UnsupportedOperationException("ComputedD is read-only");
    }

    @Override
    public double readAsDouble() {
        return dGet();
    }

    @Override
    public ArrayList<IDep> getOuts() {
        return outs;
    }

    private double recompute() {
        switch (nSources) {
            case 0:
                return SignalSupport.toDouble(f.invoke());
            case 1:
                return SignalSupport.toDouble(f.invoke(s0.readAsDouble()));
            case 2:
                return SignalSupport.toDouble(f.invoke(s0.readAsDouble(), s1.readAsDouble()));
            case 3:
                return SignalSupport.toDouble(f.invoke(
                        s0.readAsDouble(),
                        s1.readAsDouble(),
                        s2.readAsDouble()));
            default:
                double[] args = new double[nSources];
                args[0] = s0.readAsDouble();
                if (nSources > 1) {
                    args[1] = s1.readAsDouble();
                }
                if (nSources > 2) {
                    args[2] = s2.readAsDouble();
                }
                if (moreSources != null) {
                    for (int i = 0; i < moreSources.length; i++) {
                        args[3 + i] = moreSources[i].readAsDouble();
                    }
                }
                return SignalSupport.toDouble(f.applyTo(toSeq(args)));
        }
    }

    private static ISeq toSeq(double[] args) {
        ISeq seq = null;
        for (int i = args.length - 1; i >= 0; i--) {
            seq = new Cons(args[i], seq);
        }
        return seq;
    }
}
