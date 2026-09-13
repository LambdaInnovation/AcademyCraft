package cn.li.neoforge262.capability;

import java.util.Objects;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a legacy {@link IEnergyStorage} as a 26.2 {@link EnergyHandler}
 * so it can be registered on {@code Capabilities.Energy.BLOCK}.
 *
 * <p>Mutations go through receive/extract; aborted transactions restore the
 * previous stored amount by reversing the delta.</p>
 */
@SuppressWarnings("removal")
final class EnergyStorageAsHandler implements EnergyHandler {
    private final IEnergyStorage storage;
    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return storage.getEnergyStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            int current = storage.getEnergyStored();
            int target = snapshot == null ? 0 : snapshot;
            if (current > target) {
                storage.extractEnergy(current - target, false);
            } else if (current < target) {
                storage.receiveEnergy(target - current, false);
            }
        }
    };

    private EnergyStorageAsHandler(IEnergyStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    static EnergyHandler wrap(IEnergyStorage storage) {
        return storage == null ? null : new EnergyStorageAsHandler(storage);
    }

    @Override
    public long getAmountAsLong() {
        return storage.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !storage.canReceive()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return storage.receiveEnergy(amount, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !storage.canExtract()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return storage.extractEnergy(amount, false);
    }
}
