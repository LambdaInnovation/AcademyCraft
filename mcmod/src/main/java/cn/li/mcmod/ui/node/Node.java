package cn.li.mcmod.ui.node;

import cn.li.mcmod.uipojo.runtime.IUiNode;
import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;

/**
 * Mutable UI node POJO — fields + accessors only; tree logic lives in Clojure.
 */
public final class Node implements INode, IUiNode {

    private static final int INITIAL_CHILD_CAP = 4;

    private final int idx;
    private final Keyword id;
    private final Keyword kind;

    private INode parent;
    private INode[] children;
    private int childCount;

    private double x;
    private double y;
    private double w;
    private double h;
    private double scale;
    private double z;

    private double absX;
    private double absY;
    private double cumScale;

    private final double pivotX;
    private final double pivotY;
    private final byte alignW;
    private final byte alignH;

    private int flags;
    private boolean visible;

    private final IPersistentMap staticProps;

    private final double[] dslots;
    private final Object[] oslots;

    public Node(int idx, Keyword id, Keyword kind,
                INode parent, INode[] children,
                double x, double y, double w, double h, double scale, double z,
                double absX, double absY, double cumScale,
                double pivotX, double pivotY,
                byte alignW, byte alignH,
                int flags, boolean visible,
                IPersistentMap staticProps,
                double[] dslots, Object[] oslots) {
        this.idx = idx;
        this.id = id;
        this.kind = kind;
        this.parent = parent;
        this.children = children != null ? children : new INode[INITIAL_CHILD_CAP];
        this.childCount = 0;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.scale = scale;
        this.z = z;
        this.absX = absX;
        this.absY = absY;
        this.cumScale = cumScale;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.alignW = alignW;
        this.alignH = alignH;
        this.flags = flags;
        this.visible = visible;
        this.staticProps = staticProps;
        this.dslots = dslots;
        this.oslots = oslots;
    }

    @Override
    public int getIdx() {
        return idx;
    }

    @Override
    public Keyword getId() {
        return id;
    }

    @Override
    public Keyword getKind() {
        return kind;
    }

    @Override
    public INode getParentNode() {
        return parent;
    }

    @Override
    public void setParentNode(INode p) {
        parent = p;
    }

    @Override
    public INode[] getChildrenArr() {
        return children;
    }

    @Override
    public void setChildrenArr(INode[] cs) {
        children = cs;
    }

    @Override
    public int getChildCount() {
        return childCount;
    }

    @Override
    public void setChildCount(int count) {
        childCount = count;
    }

    @Override
    public INode getChild(int index) {
        return children[index];
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public void setX(double v) {
        x = v;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setY(double v) {
        y = v;
    }

    @Override
    public double getW() {
        return w;
    }

    @Override
    public void setW(double v) {
        w = v;
    }

    @Override
    public double getH() {
        return h;
    }

    @Override
    public void setH(double v) {
        h = v;
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public void setScale(double v) {
        scale = v;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public void setZ(double v) {
        z = v;
    }

    @Override
    public double getAbsX() {
        return absX;
    }

    @Override
    public void setAbsX(double v) {
        absX = v;
    }

    @Override
    public double getAbsY() {
        return absY;
    }

    @Override
    public void setAbsY(double v) {
        absY = v;
    }

    @Override
    public double getCumScale() {
        return cumScale;
    }

    @Override
    public void setCumScale(double v) {
        cumScale = v;
    }

    @Override
    public double getPivotX() {
        return pivotX;
    }

    @Override
    public void setPivotX(double px) {
        // immutable after build
    }

    @Override
    public double getPivotY() {
        return pivotY;
    }

    @Override
    public void setPivotY(double py) {
        // immutable after build
    }

    @Override
    public byte getAlignW() {
        return alignW;
    }

    @Override
    public void setAlignW(byte aw) {
        // no-op (immutable after build)
    }

    @Override
    public byte getAlignH() {
        return alignH;
    }

    @Override
    public void setAlignH(byte ah) {
        // no-op (immutable after build)
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public void setFlag(int mask) {
        flags |= mask;
    }

    @Override
    public void clearFlag(int mask) {
        flags &= ~mask;
    }

    @Override
    public boolean hasFlag(int mask) {
        return (flags & mask) != 0;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
    }

    @Override
    public IPersistentMap getStaticProps() {
        return staticProps;
    }

    @Override
    public double getDSlot(int i) {
        return dslots[i];
    }

    @Override
    public void setDSlot(int i, double v) {
        dslots[i] = v;
    }

    @Override
    public Object getOSlot(int i) {
        return oslots[i];
    }

    @Override
    public void setOSlot(int i, Object v) {
        oslots[i] = v;
    }
}
