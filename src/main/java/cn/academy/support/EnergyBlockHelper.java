package cn.academy.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

/**
 * A generic typed energyBlock helper.
 */
public class EnergyBlockHelper {
	
	private static List<IEnergyBlockManager> handlers = new ArrayList();
	
	public static void register(IEnergyBlockManager handler) {
		handlers.add(handler);
	}
	
	public static boolean isSupported(TileEntity tile) {
		for(IEnergyBlockManager handler : handlers)
			if(handler.isSupported(tile))
				return true;
		return false;
	}
	
	public static double getEnergy(TileEntity tile) {
		for(IEnergyBlockManager handler : handlers)
			if(handler.isSupported(tile))
				return handler.getEnergy(tile);
		return 0;
	}
	
	public static void setEnergy(TileEntity tile, double energy) {
		for(IEnergyBlockManager handler : handlers)
			if(handler.isSupported(tile)) {
				handler.setEnergy(tile, energy);
				break;
			}
	}
	
	public static double charge(TileEntity tile, double amt, boolean ignoreBandwidth) {
		for(IEnergyBlockManager handler : handlers)
			if(handler.isSupported(tile)) {
				return handler.charge(tile, amt, ignoreBandwidth);
			}
		return amt;
	}
	
	public static double pull(TileEntity tile, double amt, boolean ignoreBandwidth) {
		for(IEnergyBlockManager handler : handlers)
			if(handler.isSupported(tile)) {
				return handler.pull(tile, amt, ignoreBandwidth);
			}
		return 0;
	}
	
	public static interface IEnergyBlockManager {
		
		boolean isSupported(TileEntity tile);
		
		double getEnergy(TileEntity tile);
		
		void setEnergy(TileEntity tile, double energy);
		
		/**
		 * Charge a specified amount of energy into the tile.
		 * @return How much energy not charged into the tile(left)
		 */
		double charge(TileEntity tile, double amt, boolean ignoreBandwidth);
		/**
		 * Pull a specified amount of energy from the energy tile.
		 * @return How much energy pulled out
		 */
		double pull(TileEntity tile, double amt, boolean ignoreBandwidth);
		
	}
	
}
