package cn.li.mcbase.shim;

import java.util.function.Predicate;
import clojure.lang.IFn;

/** Universal adapter: Clojure IFn → java.util.function.Predicate. */
public class FnPredicate<T> implements Predicate<T> {
    private final IFn fn;
    public FnPredicate(IFn fn) { this.fn = fn; }
    @Override public boolean test(T t) { return (boolean) fn.invoke(t); }
    public static <T> FnPredicate<T> of(IFn fn) { return new FnPredicate<>(fn); }
}
