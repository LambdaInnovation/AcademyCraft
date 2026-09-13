package cn.li.mcmod.energy;

/**
 * Platform-neutral energy storage interface.
 * Methods mirror Forge's IEnergyStorage so Forge adapters can delegate without
 * ac code directly importing Forge classes.
 */
public interface IEnergyCapable {
    int receiveEnergy(int maxReceive, boolean simulate);
    int extractEnergy(int maxExtract, boolean simulate);
    int getEnergyStored();
    int getMaxEnergyStored();
    boolean canExtract();
    boolean canReceive();
}