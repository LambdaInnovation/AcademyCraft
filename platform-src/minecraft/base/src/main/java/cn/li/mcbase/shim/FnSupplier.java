package cn.li.mcbase.shim;

import java.util.function.Supplier;
import clojure.lang.IFn;

/** Universal adapter: Clojure IFn → java.util.function.Supplier. */
public class FnSupplier<T> implements Supplier<T> {
    private final IFn fn;
    public FnSupplier(IFn fn) { this.fn = fn; }
    @Override public T get() { return (T) fn.invoke(); }
    public static <T> FnSupplier<T> of(IFn fn) { return new FnSupplier<>(fn); }
}
