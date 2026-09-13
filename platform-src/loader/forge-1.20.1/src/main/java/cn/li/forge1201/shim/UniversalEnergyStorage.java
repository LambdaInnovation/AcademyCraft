package cn.li.forge1201.shim;

import net.minecraftforge.energy.IEnergyStorage;
import clojure.lang.IFn;

/** Universal IEnergyStorage skeleton — one class for ALL blocks.
 *  Pure puppet: holds IFn function pointers injected by mc-1.20.1 bridge.
 *  ac layer never sees this class — it only provides pure functions in Framework. */
public class UniversalEnergyStorage implements IEnergyStorage {
    private final IFn receiveFn;
    private final IFn extractFn;
    private final IFn getStoredFn;
    private final IFn getMaxStoredFn;
    private final IFn canExtractFn;
    private final IFn canReceiveFn;

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

    @Override public int receiveEnergy(int maxReceive, boolean simulate) {
        if (receiveFn == null) return 0;
        return ((Number) receiveFn.invoke(maxReceive, simulate)).intValue();
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) {
        if (extractFn == null) return 0;
        return ((Number) extractFn.invoke(maxExtract, simulate)).intValue();
    }

    @Override public int getEnergyStored() {
        if (getStoredFn == null) return 0;
        return ((Number) getStoredFn.invoke()).intValue();
    }

    @Override public int getMaxEnergyStored() {
        if (getMaxStoredFn == null) return 0;
        return ((Number) getMaxStoredFn.invoke()).intValue();
    }

    @Override public boolean canExtract() {
        if (canExtractFn == null) return true;
        return (boolean) canExtractFn.invoke();
    }

    @Override public boolean canReceive() {
        if (canReceiveFn == null) return true;
        return (boolean) canReceiveFn.invoke();
    }
}
