package cn.li.neoforge262.capability;

import cn.li.mcmod.energy.IEnergyCapable;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Native 26.2 energy handler backed by a content {@link IEnergyCapable}.
 *
 * Conversion: content energy unit ↔ FE (Forge Energy).
 */
public final class ForgeEnergyAdapter implements EnergyHandler {
    private final IEnergyCapable contentEnergy;
    private final double conversionRate;
    private final SnapshotJournal<Integer> journal = new SnapshotJournal<>() {
        @Override
        protected Integer createSnapshot() {
            return contentEnergy.getEnergyStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            int current = contentEnergy.getEnergyStored();
            int target = snapshot == null ? 0 : snapshot;
            if (current > target) {
                contentEnergy.extractEnergy(current - target, false);
            } else if (current < target) {
                contentEnergy.receiveEnergy(target - current, false);
            }
        }
    };

    /**
     * Create a Forge Energy adapter for a content energy capable block.
     *
     * @param contentEnergy The content energy capable implementation
     * @param conversionRate Conversion rate (1 content energy unit = X FE)
     */
    public ForgeEnergyAdapter(IEnergyCapable contentEnergy, double conversionRate) {
        if (!Double.isFinite(conversionRate) || conversionRate <= 0.0D) {
            throw new IllegalArgumentException("conversionRate must be finite and positive");
        }
        this.contentEnergy = contentEnergy;
        this.conversionRate = conversionRate;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !contentEnergy.canReceive()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        int contentAmount = toContentUnits(amount);
        int contentReceived = contentEnergy.receiveEnergy(contentAmount, false);
        return Math.min(amount, toForgeUnits(contentReceived));
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !contentEnergy.canExtract()) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        int contentAmount = toContentUnits(amount);
        int contentExtracted = contentEnergy.extractEnergy(contentAmount, false);
        return Math.min(amount, toForgeUnits(contentExtracted));
    }

    @Override
    public long getAmountAsLong() {
        return toForgeUnits(contentEnergy.getEnergyStored());
    }

    @Override
    public long getCapacityAsLong() {
        return toForgeUnits(contentEnergy.getMaxEnergyStored());
    }

    private int toContentUnits(int forgeAmount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(forgeAmount / conversionRate));
    }

    private int toForgeUnits(int contentAmount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(contentAmount * conversionRate));
    }
}
