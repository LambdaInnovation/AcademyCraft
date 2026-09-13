package cn.li.forge1201.capability;

import cn.li.mcmod.energy.IEnergyCapable;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Adapter that wraps a content IEnergyCapable and exposes it as Forge's IEnergyStorage.
 * This allows external mods using Forge Energy to interact with content energy endpoints.
 *
 * Conversion: content energy unit ↔ FE (Forge Energy).
 */
public class ForgeEnergyAdapter implements IEnergyStorage {
    private final IEnergyCapable contentEnergy;
    private final double conversionRate;

    /**
     * Create a Forge Energy adapter for a content energy capable block.
     *
     * @param contentEnergy The content energy capable implementation
     * @param conversionRate Conversion rate (1 content energy unit = X FE)
     */
    public ForgeEnergyAdapter(IEnergyCapable contentEnergy, double conversionRate) {
        this.contentEnergy = contentEnergy;
        this.conversionRate = conversionRate;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive()) {
            return 0;
        }

        double contentAmount = maxReceive / conversionRate;
        int contentReceived = contentEnergy.receiveEnergy((int) contentAmount, simulate);

        // Convert back to FE for return value
        return (int) (contentReceived * conversionRate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) {
            return 0;
        }

        double contentAmount = maxExtract / conversionRate;
        int contentExtracted = contentEnergy.extractEnergy((int) contentAmount, simulate);

        // Convert back to FE for return value
        return (int) (contentExtracted * conversionRate);
    }

    @Override
    public int getEnergyStored() {
        int contentStored = contentEnergy.getEnergyStored();
        return (int) (contentStored * conversionRate);
    }

    @Override
    public int getMaxEnergyStored() {
        int contentMax = contentEnergy.getMaxEnergyStored();
        return (int) (contentMax * conversionRate);
    }

    @Override
    public boolean canExtract() {
        return contentEnergy.canExtract();
    }

    @Override
    public boolean canReceive() {
        return contentEnergy.canReceive();
    }
}
