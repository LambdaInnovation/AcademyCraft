/**
 * 
 */
package cn.academy.core.block;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.client.gui.dev.GuiDeveloper;
import cn.academy.core.proxy.ACCommonProps;
import cn.liutils.api.EntityManipHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 能力开发机的TE
 * @author WeathFolD
 */
public class TileDeveloper extends TileEntity implements IEnergySink {
	
	public static final double INIT_MAX_ENERGY = 80000.0;
	public static final int UPDATE_RATE = 5;
	
	public double curEnergy;
	private EntityPlayer user;
	private int updateCount;
	private boolean init;

	public TileDeveloper() { }
	
	@Override
	@SideOnly(Side.CLIENT)
	public void updateEntity() {
		GuiScreen gs = Minecraft.getMinecraft().currentScreen;
		if(gs == null || !(gs instanceof GuiDeveloper)) {
			userQuit();
		}
		if(!worldObj.isRemote && !init) {
			init = !MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		}
		
		++updateCount;
		if(!worldObj.isRemote && user != null) {
			//HeartBeat update
			if(updateCount >= UPDATE_RATE) {
				updateCount = 0;
				AcademyCraftMod.netHandler.sendTo(new MsgDeveloper(this), (EntityPlayerMP) user);
			}
		}
	}
	
	/**
	 * 尝试让某个玩家使用开发机
	 * @param player
	 * @return if attempt successful
	 */
	public boolean use(EntityPlayer player) {
		if(user != null) return false;
		user = player;
		EntityManipHandler.addEntityManip(new DevPlayerManip(user, this), true);
		player.openGui(AcademyCraftMod.INSTANCE, ACCommonProps.GUI_ID_ABILITY_DEV,
				player.worldObj, xCoord, yCoord, zCoord);
		return true;
	}
	
	public void userQuit() {
		user = null;
	}
	
	public EntityPlayer getUser() {
		return user;
	}
	
	//Energy
	
	public double getMaxEnergy() {
		return INIT_MAX_ENERGY;
	}

	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter,
			ForgeDirection direction) {
		return true;
	}

	@Override
	public double demandedEnergyUnits() {
		return getMaxEnergy() - curEnergy;
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		curEnergy += amount;
		double max = getMaxEnergy();
		double lo = curEnergy - max;
		if(lo > 0) {
			curEnergy = max;
			return lo;
		}
		return 0;
	}

	@Override
	public int getMaxSafeInput() {
		return 512;
	}
	
	public float syncRateDisplay() {
		return 27.1828F;
	}
	
	public double getSyncRate() {
		return 100;
	}
	
	//Save and load
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	curEnergy = nbt.getDouble("energy");
    }
    
    public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	nbt.setDouble("energy", curEnergy);
    }
}
