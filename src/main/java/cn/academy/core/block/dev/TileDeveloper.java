/**
 * 
 */
package cn.academy.core.block.dev;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.tile.IEnergySink;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
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
		
	public static final int ID_LEVEL_UPGRADE = 0, ID_SKILL_ACQUIRE = 1, ID_DEVELOP = 2;
	public static final double INIT_MAX_ENERGY = 80000.0;
	public static final int UPDATE_RATE = 5;
	
	//Stimulation
	public static final int PER_STIM_TIME = 10;
	
	private static final List<Constructor<? extends IDevAction>> devActions = new ArrayList<Constructor<? extends IDevAction>>();
	static {
		//Add actions
		try {
			devActions.add(DevActionLevel.class.getConstructor(Integer.TYPE));
			devActions.add(DevActionSkill.class.getConstructor(Integer.TYPE));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//State for synchronization
	//Action done all in server, syncs to client
	public double curEnergy;
	
	public boolean isStimulating;
	public int maxStimTimes;
	
	public IDevAction action;
	public int stimActionID;
	public int stimSuccess;
	public int stimFailure;
	
	//Internal States
	private EntityPlayer user;
	private int updateCount;
	private boolean init;
	private int stimTicker;
	private static final Random RNG = new Random();

	public TileDeveloper() { }
	
	public static IDevAction getAction(int i, int par) {
		IDevAction res = null;
		try {
			res = devActions.get(i).newInstance(par);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	
	@Override
	@SideOnly(Side.CLIENT)
	public void updateEntity() {
		if(!worldObj.isRemote && !init) {
			init = !MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
		}
		
		if(!isHead())
			return;
		
//		if(this.isStimulating) {
//			System.out.println("Stimulating " + worldObj.isRemote);
//		}
		
		//TODO: Move to GUI event listening
		GuiScreen gs = Minecraft.getMinecraft().currentScreen;
		if(gs == null || !(gs instanceof GuiDeveloper)) {
			userQuit();
		}
		
		++updateCount;
		if(!worldObj.isRemote && user != null) {
			//HeartBeat update
			if(isStimulating) {
				updateStimulate();
			}
			
			if(updateCount >= UPDATE_RATE) {
				updateCount = 0;
				sync();
			}
		}
	}
	
	private void updateStimulate() {
		if(user == null) {
			isStimulating = false;
			return;
		}
		++stimTicker;
		if(stimTicker == PER_STIM_TIME) {
			stimTicker = 0;
			if(!perConsume()) {
				isStimulating = false;
				return;
			}
			if(RNG.nextDouble() < getSuccessProb()) {
				++stimSuccess;
				if(stimSuccess == maxStimTimes) {
					action.onActionFinished(AbilityDataMain.getData(user));
					isStimulating = false;
				}
			} else {
				++stimFailure;
			}
		}
	}
	
	private boolean perConsume() {
		return true;
	}
	
	private double getEUConsume() {
		return 0;
	}
	
	private float getExpConsume() {
		return 0F;
	}
	
	public double getSuccessProb() {
		return 0.7;
	}
	
	private TileDeveloper getHead() {
		BlockDeveloper bd = (BlockDeveloper) getBlockType();
		if(!isHead()) {
			int[] coords = bd.getOrigin(worldObj, xCoord, yCoord, zCoord, getBlockMetadata());
			TileEntity td = worldObj.getTileEntity(xCoord, yCoord, zCoord);
			if(td == null || !(td instanceof TileDeveloper)) { //Silent error processing (Is it good?)
				return this;
			}
			return (TileDeveloper) td;
		}
		return this;
	}
	
	private boolean isHead() {
		return getBlockMetadata() >> 2 == 0;
	}
	
	/**
	 * Start stimulating at server with the given action ID.
	 */
	public void startStimulating(int id, int par) {
		if(user == null) {
			throw new RuntimeException("Developing without user");
		}
		action = getAction(id, par);
		AbilityData data = AbilityDataMain.getData(user);
		isStimulating = true;
		maxStimTimes = action.getExpectedStims(data);
		stimSuccess = stimFailure = 0;
		sync(); //Force update if server
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
	
	private void sync() {
		if(!worldObj.isRemote) {
			//System.out.println("sync");
			AcademyCraftMod.netHandler.sendTo(new MsgDeveloper(this), (EntityPlayerMP) user);
		}
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
		return getHead().realDEU();
	}
	
	private double realDEU() {
		return getMaxEnergy() - curEnergy;
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		return getHead().realIEU(directionFrom, amount);
	}
	
	private double realIEU(ForgeDirection directionFrom, double amount) {
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
