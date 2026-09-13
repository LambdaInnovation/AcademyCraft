package cn.li.neoforge262.capability;

import java.util.Objects;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a legacy {@link IFluidHandler} as a 26.2
 * {@link ResourceHandler}{@code <FluidResource>} for
 * {@code Capabilities.Fluid.BLOCK}.
 */
@SuppressWarnings("removal")
final class FluidHandlerAsResourceHandler implements ResourceHandler<FluidResource> {
    private final IFluidHandler handler;
    private final SnapshotJournal<FluidStack[]> journal = new SnapshotJournal<>() {
        @Override
        protected FluidStack[] createSnapshot() {
            int tanks = handler.getTanks();
            FluidStack[] snap = new FluidStack[tanks];
            for (int i = 0; i < tanks; i++) {
                FluidStack stack = handler.getFluidInTank(i);
                snap[i] = stack == null || stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
            }
            return snap;
        }

        @Override
        protected void revertToSnapshot(FluidStack[] snapshot) {
            if (snapshot == null) {
                return;
            }
            int tanks = Math.min(handler.getTanks(), snapshot.length);
            for (int i = 0; i < tanks; i++) {
                FluidStack current = handler.getFluidInTank(i);
                if (current != null && !current.isEmpty()) {
                    handler.drain(current.copy(), IFluidHandler.FluidAction.EXECUTE);
                }
                FluidStack target = snapshot[i];
                if (target != null && !target.isEmpty()) {
                    handler.fill(target.copy(), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    };

    private FluidHandlerAsResourceHandler(IFluidHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    static ResourceHandler<FluidResource> wrap(IFluidHandler handler) {
        return handler == null ? null : new FluidHandlerAsResourceHandler(handler);
    }

    @Override
    public int size() {
        return handler.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        FluidStack stack = handler.getFluidInTank(index);
        return stack == null || stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        FluidStack stack = handler.getFluidInTank(index);
        return stack == null ? 0L : stack.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return handler.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        if (resource == null || resource.isEmpty()) {
            return false;
        }
        return handler.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!isValid(index, resource)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return handler.fill(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidStack current = handler.getFluidInTank(index);
        if (current == null || current.isEmpty() || !resource.matches(current)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        FluidStack drained = handler.drain(resource.toStack(amount), IFluidHandler.FluidAction.EXECUTE);
        return drained == null ? 0 : drained.getAmount();
    }
}
