package cn.li.neoforge262.shim;

import clojure.lang.IFn;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Universal 26.2 {@link EnergyHandler} skeleton — one class for all blocks. */
public final class UniversalEnergyStorage implements EnergyHandler {
    private final IFn receiveFn;
    private final IFn extractFn;
    private final IFn getStoredFn;
    private final IFn getMaxStoredFn;
    private final IFn canExtractFn;
    private final IFn canReceiveFn;
    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            int current = stored();
            int target = snapshot == null ? 0 : snapshot;
            if (current > target && extractFn != null) {
                extractFn.invoke(current - target, false);
            } else if (current < target && receiveFn != null) {
                receiveFn.invoke(target - current, false);
            }
        }
    };

    public UniversalEnergyStorage(IFn receiveFn, IFn extractFn,
                                   IFn getStoredFn, IFn getMaxStoredFn,
                                   IFn canExtractFn, IFn canReceiveFn) {
        this.receiveFn = receiveFn;
        this.extractFn = extractFn;
        this.getStoredFn = getStoredFn;
        this.getMaxStoredFn = getMaxStoredFn;
        this.canExtractFn = canExtractFn;
        this.canReceiveFn = canReceiveFn;
    }

    private int stored() {
        return getStoredFn == null ? 0 : ((Number) getStoredFn.invoke()).intValue();
    }

    @Override
    public long getAmountAsLong() {
        return stored();
    }

    @Override
    public long getCapacityAsLong() {
        return getMaxStoredFn == null ? 0 : ((Number) getMaxStoredFn.invoke()).longValue();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || receiveFn == null || !canReceive()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return ((Number) receiveFn.invoke(amount, false)).intValue();
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || extractFn == null || !canExtract()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return ((Number) extractFn.invoke(amount, false)).intValue();
    }

    private boolean canExtract() {
        return canExtractFn == null || (boolean) canExtractFn.invoke();
    }

    private boolean canReceive() {
        return canReceiveFn == null || (boolean) canReceiveFn.invoke();
    }
}
