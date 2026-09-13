package cn.li.mcmod.uipojo.runtime;

import cn.li.mcmod.uipojo.runtime.IUiNode;
import cn.li.mcmod.uipojo.signal.Binding;
import cn.li.mcmod.uipojo.signal.SigD;
import cn.li.mcmod.uipojo.signal.SigL;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentArrayMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * UI runtime — one instance per screen or overlay.
 * Java POJO so Clojure AOT (reflection=fail) can access mutable state without deftype field reflection.
 */
public final class UiRt {

    private final SigL clockMs;
    private final SigD partialTicks;
    private final SigL gameTicks;
    private final ArrayList<IUiNode> nodes;
    private IPersistentMap idToNode;
    private final ArrayList<Binding> dirtyBindings;
    private final HashMap<Integer, ArrayList<Binding>> bindingsByNode;
    private Object[] tape;
    private boolean treeDirty;
    private double screenW;
    private double screenH;
    private int hoveredIdx;
    private int focusIdx;
    private int dragNodeIdx;
    private boolean dragging;
    private double dragStartMx;
    private double dragStartMy;
    private long dragStartMs;
    private IPersistentMap events;
    private IPersistentMap userSignals;
    private boolean disposed;

    public UiRt(SigL clockMs, SigD partialTicks, SigL gameTicks) {
        this.clockMs = clockMs;
        this.partialTicks = partialTicks;
        this.gameTicks = gameTicks;
        this.nodes = new ArrayList<>(64);
        this.idToNode = PersistentArrayMap.EMPTY;
        this.dirtyBindings = new ArrayList<>(32);
        this.bindingsByNode = new HashMap<>(32);
        this.tape = new Object[0];
        this.treeDirty = true;
        this.screenW = 0.0;
        this.screenH = 0.0;
        this.hoveredIdx = -1;
        this.focusIdx = -1;
        this.dragNodeIdx = -1;
        this.dragging = false;
        this.dragStartMx = 0.0;
        this.dragStartMy = 0.0;
        this.dragStartMs = 0L;
        this.events = PersistentArrayMap.EMPTY;
        this.userSignals = PersistentArrayMap.EMPTY;
        this.disposed = false;
    }

    public SigL getClockMs() { return clockMs; }
    public SigD getPartialTicks() { return partialTicks; }
    public SigL getGameTicks() { return gameTicks; }

    public ArrayList<IUiNode> getNodes() { return nodes; }

    public IPersistentMap getIdToNode() { return idToNode; }
    public void setIdToNode(IPersistentMap idToNode) { this.idToNode = idToNode; }

    public ArrayList<Binding> getDirtyBindings() { return dirtyBindings; }
    public HashMap<Integer, ArrayList<Binding>> getBindingsByNode() { return bindingsByNode; }

    public Object[] getTape() { return tape; }
    public void setTape(Object[] tape) { this.tape = tape; }

    public boolean isTreeDirty() { return treeDirty; }
    public void setTreeDirty(boolean treeDirty) { this.treeDirty = treeDirty; }

    public double getScreenW() { return screenW; }
    public void setScreenW(double screenW) { this.screenW = screenW; }

    public double getScreenH() { return screenH; }
    public void setScreenH(double screenH) { this.screenH = screenH; }

    public int getHoveredIdx() { return hoveredIdx; }
    public void setHoveredIdx(int hoveredIdx) { this.hoveredIdx = hoveredIdx; }

    public int getFocusIdx() { return focusIdx; }
    public void setFocusIdx(int focusIdx) { this.focusIdx = focusIdx; }

    public int getDragNodeIdx() { return dragNodeIdx; }
    public void setDragNodeIdx(int dragNodeIdx) { this.dragNodeIdx = dragNodeIdx; }

    public boolean isDragging() { return dragging; }
    public void setDragging(boolean dragging) { this.dragging = dragging; }

    public double getDragStartMx() { return dragStartMx; }
    public void setDragStartMx(double dragStartMx) { this.dragStartMx = dragStartMx; }

    public double getDragStartMy() { return dragStartMy; }
    public void setDragStartMy(double dragStartMy) { this.dragStartMy = dragStartMy; }

    public long getDragStartMs() { return dragStartMs; }
    public void setDragStartMs(long dragStartMs) { this.dragStartMs = dragStartMs; }

    public IPersistentMap getEvents() { return events; }
    public void setEvents(IPersistentMap events) { this.events = events; }

    public IPersistentMap getUserSignals() { return userSignals; }
    public void setUserSignals(IPersistentMap userSignals) { this.userSignals = userSignals; }

    public boolean isDisposed() { return disposed; }
    public void setDisposed(boolean disposed) { this.disposed = disposed; }
}
