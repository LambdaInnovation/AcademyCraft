package cn.li.mcbase.shim;

import java.util.function.Consumer;
import clojure.lang.IFn;

/** Universal adapter: Clojure IFn → java.util.function.Consumer.
 *  One class serves all Consumer reify sites in the project. */
public class FnConsumer<T> implements Consumer<T> {
    private final IFn fn;
    public FnConsumer(IFn fn) { this.fn = fn; }
    @Override public void accept(T t) { fn.invoke(t); }
    public static <T> FnConsumer<T> of(IFn fn) { return new FnConsumer<>(fn); }
}
