package cn.li.mcbase.shim;

import clojure.lang.IFn;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Java skeleton for {@link Container} — replaces Clojure reify on MC interfaces (AOT / iron law 7).
 */
public class UniversalContainer implements Container {
    private final Object cljContainer;
    private final IFn slotCountFn;
    private final IFn slotGetItemFn;
    private final IFn slotSetItemFn;
    private final IFn slotChangedFn;
    private final IFn safeValidateFn;
    private final IFn slotCanPlaceFn;

    public UniversalContainer(Object cljContainer,
                              IFn slotCountFn,
                              IFn slotGetItemFn,
                              IFn slotSetItemFn,
                              IFn slotChangedFn,
                              IFn safeValidateFn,
                              IFn slotCanPlaceFn) {
        this.cljContainer = cljContainer;
        this.slotCountFn = slotCountFn;
        this.slotGetItemFn = slotGetItemFn;
        this.slotSetItemFn = slotSetItemFn;
        this.slotChangedFn = slotChangedFn;
        this.safeValidateFn = safeValidateFn;
        this.slotCanPlaceFn = slotCanPlaceFn;
    }

    private int slotCount() {
        if (slotCountFn == null) return 0;
        Object n = slotCountFn.invoke(cljContainer);
        return n == null ? 0 : ((Number) n).intValue();
    }

    @Override
    public int getContainerSize() {
        return slotCount();
    }

    @Override
    public boolean isEmpty() {
        int n = slotCount();
        for (int idx = 0; idx < n; idx++) {
            ItemStack stack = getItem(idx);
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slotGetItemFn == null) return ItemStack.EMPTY;
        ItemStack stack = (ItemStack) slotGetItemFn.invoke(cljContainer, slot);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.split(amount);
        if (current.isEmpty()) {
            if (slotSetItemFn != null) slotSetItemFn.invoke(cljContainer, slot, null);
        } else {
            if (slotSetItemFn != null) slotSetItemFn.invoke(cljContainer, slot, current);
        }
        if (slotChangedFn != null) slotChangedFn.invoke(cljContainer, slot);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (slotSetItemFn != null) slotSetItemFn.invoke(cljContainer, slot, null);
        if (slotChangedFn != null) slotChangedFn.invoke(cljContainer, slot);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slotSetItemFn != null) slotSetItemFn.invoke(cljContainer, slot, stack);
        if (slotChangedFn != null) slotChangedFn.invoke(cljContainer, slot);
    }

    @Override
    public void setChanged() {
        if (slotChangedFn == null) return;
        int n = slotCount();
        for (int idx = 0; idx < n; idx++) {
            slotChangedFn.invoke(cljContainer, idx);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (safeValidateFn == null) return true;
        Object result = safeValidateFn.invoke(cljContainer, player);
        return result != null && (boolean) result;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slotCanPlaceFn == null) return true;
        Object result = slotCanPlaceFn.invoke(cljContainer, slot, stack);
        return result != null && (boolean) result;
    }

    @Override
    public void clearContent() {
        int n = slotCount();
        for (int idx = 0; idx < n; idx++) {
            if (slotSetItemFn != null) slotSetItemFn.invoke(cljContainer, idx, null);
        }
        for (int idx = 0; idx < n; idx++) {
            if (slotChangedFn != null) slotChangedFn.invoke(cljContainer, idx);
        }
    }
}
