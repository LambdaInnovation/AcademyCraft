package cn.li.mcbase.gui;

import net.minecraft.world.inventory.DataSlot;

/**
 * DataSlot backed by explicit getter/setter callbacks (wired from Clojure atoms).
 */
public final class AtomBackedDataSlot extends DataSlot {
    private final Getter getter;
    private final Setter setter;

    @FunctionalInterface
    public interface Getter {
        int getAsInt();
    }

    @FunctionalInterface
    public interface Setter {
        void accept(int value);
    }

    public AtomBackedDataSlot(Getter getter, Setter setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public int get() {
        return getter.getAsInt();
    }

    @Override
    public void set(int value) {
        setter.accept(value);
    }
}
