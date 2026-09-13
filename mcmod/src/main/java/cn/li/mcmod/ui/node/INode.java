package cn.li.mcmod.ui.node;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;

/**
 * UI node API — Java interface for cross-ns/module typed access (AOT/remap-safe).
 */
public interface INode {

    // Identity
    int getIdx();

    Keyword getId();

    Keyword getKind();

    // Hierarchy
    INode getParentNode();

    void setParentNode(INode p);

    INode[] getChildrenArr();

    void setChildrenArr(INode[] cs);

    int getChildCount();

    void setChildCount(int count);

    INode getChild(int index);

    // Layout inputs
    double getX();

    void setX(double x);

    double getY();

    void setY(double y);

    double getW();

    void setW(double w);

    double getH();

    void setH(double h);

    double getScale();

    void setScale(double s);

    double getZ();

    void setZ(double z);

    // Layout cache
    double getAbsX();

    void setAbsX(double ax);

    double getAbsY();

    void setAbsY(double ay);

    double getCumScale();

    void setCumScale(double cs);

    // Static layout meta
    double getPivotX();

    void setPivotX(double px);

    double getPivotY();

    void setPivotY(double py);

    byte getAlignW();

    void setAlignW(byte aw);

    byte getAlignH();

    void setAlignH(byte ah);

    // Flags
    int getFlags();

    void setFlag(int mask);

    void clearFlag(int mask);

    boolean hasFlag(int mask);

    // Visibility
    boolean isVisible();

    void setVisible(boolean v);

    // Static props
    IPersistentMap getStaticProps();

    // Dynamic slots (dslots = primitives; oslots = heterogeneous refs)
    double getDSlot(int i);

    void setDSlot(int i, double v);

    Object getOSlot(int i);

    void setOSlot(int i, Object v);
}
