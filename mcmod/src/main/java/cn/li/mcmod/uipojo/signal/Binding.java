package cn.li.mcmod.uipojo.signal;

import cn.li.mcmod.uipojo.runtime.IUiNode;
import clojure.lang.IFn;
import java.util.ArrayList;

public final class Binding implements IDep, IApply {

    private final ISupportsOuts source;
    private final IUiNode node;
    private final IFn applyFn;
    private boolean queued;
    private final ArrayList<Binding> flushQueue;

    public Binding(ISupportsOuts source,
                   IUiNode node,
                   IFn applyFn,
                   boolean queued,
                   ArrayList<Binding> flushQueue) {
        this.source = source;
        this.node = node;
        this.applyFn = applyFn;
        this.queued = queued;
        this.flushQueue = flushQueue;
    }

    public ISupportsOuts getSource() {
        return source;
    }

    public IUiNode getNode() {
        return node;
    }

    @Override
    public void depMarkDirty() {
        if (!queued) {
            queued = true;
            flushQueue.add(this);
        }
    }

    @Override
    public void applyBinding() {
        queued = false;
        applyFn.invoke(node, source);
    }
}
