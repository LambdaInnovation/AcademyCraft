package cn.li.neoforge262.shim;

import clojure.lang.IFn;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Native 26.2 item resource handler backed by Clojure function pointers. */
public final class UniversalItemHandler implements ResourceHandler<ItemResource> {
    private final IFn getSlotsFn;
    private final IFn getStackInSlotFn;
    private final IFn insertItemFn;
    private final IFn extractItemFn;
    private final IFn getSlotLimitFn;
    private final IFn isItemValidFn;
    private final SnapshotJournal<ItemStack[]> journal = new SnapshotJournal<>() {
        @Override
        protected ItemStack[] createSnapshot() {
            int slots = size();
            ItemStack[] snapshot = new ItemStack[slots];
            for (int i = 0; i < slots; i++) {
                ItemStack stack = stackInSlot(i);
                snapshot[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            return snapshot;
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            if (snapshot == null) {
                return;
            }
            int slots = Math.min(size(), snapshot.length);
            for (int i = 0; i < slots; i++) {
                ItemStack current = stackInSlot(i);
                if (!current.isEmpty() && extractItemFn != null) {
                    extractItemFn.invoke(i, current.getCount(), false);
                }
                ItemStack target = snapshot[i];
                if (target != null && !target.isEmpty() && insertItemFn != null) {
                    insertItemFn.invoke(i, target.copy(), false);
                }
            }
        }
    };

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

    @Override
    public int size() {
        return getSlotsFn != null ? ((Number) getSlotsFn.invoke()).intValue() : 0;
    }

    private ItemStack stackInSlot(int slot) {
        ItemStack stack = getStackInSlotFn == null ? null : (ItemStack) getStackInSlotFn.invoke(slot);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack stack = stackInSlot(index);
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return stackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return getSlotLimitFn != null ? ((Number) getSlotLimitFn.invoke(index)).longValue() : 64L;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource != null
            && !resource.isEmpty()
            && (isItemValidFn == null || (boolean) isItemValidFn.invoke(index, resource.toStack(1)));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || insertItemFn == null || !isValid(index, resource)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        ItemStack remaining = (ItemStack) insertItemFn.invoke(index, resource.toStack(amount), false);
        int left = remaining == null || remaining.isEmpty() ? 0 : remaining.getCount();
        return Math.max(0, amount - left);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack current = stackInSlot(index);
        if (amount == 0 || extractItemFn == null || current.isEmpty() || !resource.matches(current)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        ItemStack extracted = (ItemStack) extractItemFn.invoke(index, amount, false);
        return extracted == null || extracted.isEmpty() ? 0 : extracted.getCount();
    }
}
