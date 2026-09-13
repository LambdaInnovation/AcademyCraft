package cn.li.forge1201.shim;

import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.item.ItemStack;
import clojure.lang.IFn;

/** Universal IItemHandler skeleton — one class for ALL blocks.
 *  Pure puppet: injected with IFn function pointers from Framework via mc-1.20.1 bridge.
 *  ac layer never sees this class. */
public class UniversalItemHandler implements IItemHandler {
    private final IFn getSlotsFn;
    private final IFn getStackInSlotFn;
    private final IFn insertItemFn;
    private final IFn extractItemFn;
    private final IFn getSlotLimitFn;
    private final IFn isItemValidFn;

    public UniversalItemHandler(IFn getSlotsFn, IFn getStackInSlotFn,
                                 IFn insertItemFn, IFn extractItemFn,
                                 IFn getSlotLimitFn, IFn isItemValidFn) {
        this.getSlotsFn = getSlotsFn;
        this.getStackInSlotFn = getStackInSlotFn;
        this.insertItemFn = insertItemFn;
        this.extractItemFn = extractItemFn;
        this.getSlotLimitFn = getSlotLimitFn;
        this.isItemValidFn = isItemValidFn;
    }

    @Override public int getSlots() {
        return getSlotsFn != null ? ((Number) getSlotsFn.invoke()).intValue() : 0;
    }

    @Override public ItemStack getStackInSlot(int slot) {
        return getStackInSlotFn != null ? (ItemStack) getStackInSlotFn.invoke(slot) : ItemStack.EMPTY;
    }

    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return insertItemFn != null ? (ItemStack) insertItemFn.invoke(slot, stack, simulate) : stack;
    }

    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItemFn != null ? (ItemStack) extractItemFn.invoke(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override public int getSlotLimit(int slot) {
        return getSlotLimitFn != null ? ((Number) getSlotLimitFn.invoke(slot)).intValue() : 64;
    }

    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return isItemValidFn == null || (boolean) isItemValidFn.invoke(slot, stack);
    }
}
