package cn.li.neoforge262.capability;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a legacy {@link IItemHandler} as a 26.2
 * {@link ResourceHandler}{@code <ItemResource>} for
 * {@code Capabilities.Item.BLOCK}.
 */
@SuppressWarnings("removal")
final class ItemHandlerAsResourceHandler implements ResourceHandler<ItemResource> {
    private final IItemHandler handler;
    private final SnapshotJournal<ItemStack[]> journal = new SnapshotJournal<>() {
        @Override
        protected ItemStack[] createSnapshot() {
            int slots = handler.getSlots();
            ItemStack[] snap = new ItemStack[slots];
            for (int i = 0; i < slots; i++) {
                ItemStack stack = handler.getStackInSlot(i);
                snap[i] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            return snap;
        }

        @Override
        protected void revertToSnapshot(ItemStack[] snapshot) {
            if (snapshot == null) {
                return;
            }
            int slots = Math.min(handler.getSlots(), snapshot.length);
            for (int i = 0; i < slots; i++) {
                ItemStack current = handler.getStackInSlot(i);
                if (current != null && !current.isEmpty()) {
                    handler.extractItem(i, current.getCount(), false);
                }
                ItemStack target = snapshot[i];
                if (target != null && !target.isEmpty()) {
                    handler.insertItem(i, target.copy(), false);
                }
            }
        }
    };

    private ItemHandlerAsResourceHandler(IItemHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    static ResourceHandler<ItemResource> wrap(IItemHandler handler) {
        return handler == null ? null : new ItemHandlerAsResourceHandler(handler);
    }

    @Override
    public int size() {
        return handler.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        ItemStack stack = handler.getStackInSlot(index);
        return stack == null || stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        ItemStack stack = handler.getStackInSlot(index);
        return stack == null ? 0L : stack.getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getSlotLimit(index);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource == null || resource.isEmpty()) {
            return false;
        }
        return handler.isItemValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!isValid(index, resource)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        ItemStack remaining = handler.insertItem(index, resource.toStack(amount), false);
        int left = remaining == null || remaining.isEmpty() ? 0 : remaining.getCount();
        return Math.max(0, amount - left);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack current = handler.getStackInSlot(index);
        if (current == null || current.isEmpty() || !resource.matches(current)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        ItemStack extracted = handler.extractItem(index, amount, false);
        return extracted == null || extracted.isEmpty() ? 0 : extracted.getCount();
    }
}
