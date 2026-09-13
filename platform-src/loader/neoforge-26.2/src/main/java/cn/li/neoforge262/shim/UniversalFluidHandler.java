package cn.li.neoforge262.shim;

import clojure.lang.IFn;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Native 26.2 fluid resource handler backed by Clojure function pointers.
 */
public final class UniversalFluidHandler implements ResourceHandler<FluidResource> {
    private final IFn sizeFn;
    private final IFn getResourceFn;
    private final IFn getAmountFn;
    private final IFn getCapacityFn;
    private final IFn isValidFn;
    private final IFn insertFn;
    private final IFn extractFn;
    private final SnapshotJournal<TankSnapshot[]> journal = new SnapshotJournal<>() {
        @Override
        protected TankSnapshot[] createSnapshot() {
            int size = size();
            TankSnapshot[] snapshot = new TankSnapshot[size];
            for (int i = 0; i < size; i++) {
                snapshot[i] = new TankSnapshot(getResource(i), getAmountAsLong(i));
            }
            return snapshot;
        }

        @Override
        protected void revertToSnapshot(TankSnapshot[] snapshot) {
            if (snapshot == null) {
                return;
            }
            int size = Math.min(size(), snapshot.length);
            for (int i = 0; i < size; i++) {
                FluidResource current = getResource(i);
                long currentAmount = getAmountAsLong(i);
                if (!current.isEmpty() && currentAmount > 0L) {
                    invokeAmount(extractFn, i, current, currentAmount);
                }
                TankSnapshot target = snapshot[i];
                if (target != null && !target.resource().isEmpty() && target.amount() > 0L) {
                    invokeAmount(insertFn, i, target.resource(), target.amount());
                }
            }
        }
    };

    public UniversalFluidHandler(IFn sizeFn,
                                 IFn getResourceFn,
                                 IFn getAmountFn,
                                 IFn getCapacityFn,
                                 IFn isValidFn,
                                 IFn insertFn,
                                 IFn extractFn) {
        this.sizeFn = sizeFn;
        this.getResourceFn = getResourceFn;
        this.getAmountFn = getAmountFn;
        this.getCapacityFn = getCapacityFn;
        this.isValidFn = isValidFn;
        this.insertFn = insertFn;
        this.extractFn = extractFn;
    }

    @Override
    public int size() {
        return sizeFn == null ? 0 : ((Number) sizeFn.invoke()).intValue();
    }

    @Override
    public FluidResource getResource(int index) {
        if (getResourceFn == null) {
            return FluidResource.EMPTY;
        }
        FluidResource resource = (FluidResource) getResourceFn.invoke(index);
        return resource == null ? FluidResource.EMPTY : resource;
    }

    @Override
    public long getAmountAsLong(int index) {
        return getAmountFn == null ? 0L : ((Number) getAmountFn.invoke(index)).longValue();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return getCapacityFn == null ? 0L : ((Number) getCapacityFn.invoke(index, resource)).longValue();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return isValidFn == null || (boolean) isValidFn.invoke(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || insertFn == null || !isValid(index, resource)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return invokeAmount(insertFn, index, resource, amount);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0 || extractFn == null || !resource.equals(getResource(index))) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return invokeAmount(extractFn, index, resource, amount);
    }

    private static int invokeAmount(IFn fn, int index, FluidResource resource, long amount) {
        if (fn == null || amount <= 0L) {
            return 0;
        }
        int bounded = (int) Math.min(Integer.MAX_VALUE, amount);
        return ((Number) fn.invoke(index, resource, bounded)).intValue();
    }

    private record TankSnapshot(FluidResource resource, long amount) {
    }
}
