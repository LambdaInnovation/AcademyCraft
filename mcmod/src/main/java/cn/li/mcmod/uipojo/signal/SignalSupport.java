package cn.li.mcmod.uipojo.signal;

import clojure.lang.RT;
import java.util.ArrayList;
import java.util.Objects;

public final class SignalSupport {

    private SignalSupport() {}

    public static ArrayList<IDep> newOuts(int capacity) {
        return new ArrayList<>(capacity);
    }

    public static ArrayList<IDep> outsOf(ISupportsOuts signal) {
        return signal.getOuts();
    }

    public static void addDep(ArrayList<IDep> outs, IDep dep) {
        outs.add(dep);
    }

    public static void removeDep(ArrayList<IDep> outs, IDep dep) {
        outs.remove(dep);
    }

    static void notifyOuts(ArrayList<IDep> outs) {
        int n = outs.size();
        for (int i = 0; i < n; i++) {
            outs.get(i).depMarkDirty();
        }
    }

    static double toDouble(Object v) {
        return RT.doubleCast(v);
    }

    static boolean objectEquals(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
