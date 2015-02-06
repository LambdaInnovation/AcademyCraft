/**
 * 
 */
package cn.academy.core.util;

import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.ISpecialElectricItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.api.energy.IWirelessReceiver;
import cn.academy.api.energy.IWirelessTile;

/**
 * @author WeathFolD
 *
 */
public class EnergyUtils {

	public static boolean isElecBlock(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return (te instanceof IEnergyTile || te instanceof IWirelessTile);
	}
	
	public static boolean isReceiver(World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(x, y, z);
		return (te instanceof IEnergySink || te instanceof IWirelessReceiver);
	}
	
	public static boolean tryCharge(World world, int x, int y, int z, double amt) {
		TileEntity te = world.getTileEntity(x, y, z);
		if(te instanceof IEnergySink) {
			IEnergySink ies = (IEnergySink) te;
			ies.injectEnergyUnits(ForgeDirection.DOWN, amt);
			return true;
		} else if(te instanceof IWirelessReceiver) {
			IWirelessReceiver iwr = (IWirelessReceiver) te;
			iwr.injectEnergy(amt);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean isElecItem(ItemStack stack) {
		return stack.getItem() instanceof ISpecialElectricItem;
	}
	
	public static void tryCharge(ItemStack stack, int amt) {
		if(stack.getItem() instanceof ISpecialElectricItem) {
			ISpecialElectricItem iei = (ISpecialElectricItem) stack.getItem();
			iei.getManager(stack).charge(stack, amt, 3, true, false);
		}
	}

}
