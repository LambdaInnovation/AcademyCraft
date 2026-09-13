package cn.li.mcmod.uipojo.signal;

import clojure.lang.Cons;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import java.util.ArrayList;

/**
 * Universal object-valued computed signal.
 * Sources may be ISigO, IDoubleSource, or ISigD — recompute() dispatches on type.
 * Dependency tracking uses ISupportsOuts only (type-agnostic).
 */
public final class ComputedO implements ISigO, IDep, ISupportsOuts {

    private final Object s0;
    private final Object s1;
    private final Object s2;
    private final Object[] moreSources;
    private final IFn f;
    private final int nSources;
    private Object value;
    private boolean dirty;
    private final ArrayList<IDep> outs;

    public ComputedO(Object s0,
                     Object s1,
                     Object s2,
                     Object[] moreSources,
                     IFn f,
                     int nSources,
                     Object value,
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
        // See ComputedD.depMarkDirty: intentionally unguarded so a bound-but-
        // never-read computed still propagates its first source change. Binding's
        // `queued` flag keeps enqueue idempotent.
        dirty = true;
        SignalSupport.notifyOuts(outs);
    }

    @Override
    public Object sGet() {
        if (dirty) {
            dirty = false;
            value = recompute();
        }
        return value;
    }

    @Override
    public void sSet(Object v) {
        throw new UnsupportedOperationException("ComputedO is read-only");
    }

    @Override
    public ArrayList<IDep> getOuts() {
        return outs;
    }

    private Object recompute() {
        switch (nSources) {
            case 0:
                return f.invoke();
            case 1:
                return f.invoke(readSource(s0));
            case 2:
                return f.invoke(readSource(s0), readSource(s1));
            case 3:
                return f.invoke(readSource(s0), readSource(s1), readSource(s2));
            default:
                Object[] args = new Object[nSources];
                args[0] = readSource(s0);
                if (nSources > 1) {
                    args[1] = readSource(s1);
                }
                if (nSources > 2) {
                    args[2] = readSource(s2);
                }
                if (moreSources != null) {
                    for (int i = 0; i < moreSources.length; i++) {
                        args[3 + i] = readSource(moreSources[i]);
                    }
                }
                return f.applyTo(toSeq(args));
        }
    }

    private static Object readSource(Object src) {
        if (src instanceof ISigO)
            return ((ISigO) src).sGet();
        if (src instanceof IDoubleSource)
            return ((IDoubleSource) src).readAsDouble();
        if (src instanceof ISigD)
            return ((ISigD) src).dGet();
        throw new IllegalArgumentException(
            "ComputedO: unsupported source type " + src.getClass().getName());
    }

    private static ISeq toSeq(Object[] args) {
        ISeq seq = null;
        for (int i = args.length - 1; i >= 0; i--) {
            seq = new Cons(args[i], seq);
        }
        return seq;
    }
}
