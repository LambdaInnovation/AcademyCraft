/**
 * 
 */
package cn.academy.core.block.dev;

import ic2.api.energy.tile.IEnergySink;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.gui.dev.GuiDeveloper;
import cn.academy.core.client.render.RenderDeveloper;
import cn.academy.core.register.ACBlocks;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.template.block.TileGenericSink;
import cn.liutils.template.entity.EntitySittable;
import cn.liutils.template.entity.EntitySittable.ISittable;
import cn.liutils.util.DebugUtils;
import cn.liutils.util.ExpUtils;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.misc.Pair;
import cn.liutils.util.space.BlockPos;
import cn.liutils.util.space.IBlockFilter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 能力开发机的TE
 * @author WeathFolD
 */
@RegistrationClass
@RegTileEntity
@RegTileEntity.HasRender
public class TileDeveloper extends TileGenericSink implements IEnergySink, ISittable {

	public static final double INIT_MAX_ENERGY = 80000.0;
	public static final int UPDATE_RATE = 5;
	
	//Stimulation
	public static final int ID_LEVEL_UPGRADE = 0, ID_SKILL_ACQUIRE = 1, ID_DEVELOP = 2;
	public static final int PER_STIM_TIME = 10;
	
	//Module search
	public static final double MODULE_SEARCH_RANGE = 8;
	
	private static final List<Constructor<? extends IDevAction>> devActions = new ArrayList<Constructor<? extends IDevAction>>();
	static {
		//Add actions
		try {
			devActions.add(DevActionLevel.class.getConstructor(Integer.TYPE));
			devActions.add(DevActionSkill.class.getConstructor(Integer.TYPE));
			devActions.add(DevActionDevelop.class.getConstructor(Integer.TYPE));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//State for synchronization
	//Action done all in server, syncs to client
	public boolean isStimulating;
	public int maxStimTimes;
	
	public IDevAction action;
	public int stimActionID;
	public int stimSuccess;
	public int stimFailure;
	
	//brief info of components, need synchronizing
	int nMagIncr;
	
	//Internal States
	private EntityPlayer user;
	private int updateCount;
	private int stimTicker;
	private static final Random RNG = new Random();

	public TileDeveloper() {}
	
	//Sit and use API
	
	EntitySittable es;
	
	/**
	 * Let a player use this ability dev.
	 * @return if attempt successful
	 */
	public boolean use(EntityPlayer player) {
		if(user != null) return false;
		user = player;
		user.getEntityData().setBoolean("developing", true);
		if(!worldObj.isRemote)
			es.mount(user);
		guiHandler.openGuiContainer(player, worldObj, xCoord, yCoord, zCoord);
		return true;
	}
	
	public void userQuit() {
		if(user == null)
			return;
		if(!worldObj.isRemote)
			es.disMount();
		user.getEntityData().setBoolean("developing", false);
		user = null;
	}
	
	public EntityPlayer getUser() {
		return user;
	}
	
	//Stimulation API
	public static IDevAction getAction(int i, int par) {
		IDevAction res = null;
		try {
			res = devActions.get(i).newInstance(par);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return res;
	}
	
	private void updateStimulate() {
		if(user == null) {
			isStimulating = false;
			return;
		}
		++stimTicker;
		if(stimTicker == PER_STIM_TIME) {
			stimTicker = 0;
			if(!doConsume()) {
				isStimulating = false;
				return;
			}
			AbilityData data = AbilityDataMain.getData(user);
			if(RNG.nextDouble() < getSuccessProb(data)) {
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
	
	/**
	 * Handle the consumption of a single stimulation.
	 * @return if consumed successfully
	 */
	private boolean doConsume() {
		double ceu = getEUConsume();
		int cexp = getExpConsume();
		if(curEnergy < ceu || !ExpUtils.consumeExp(user, cexp))
			return false;
		
		curEnergy -= ceu;
		return true;
	}
	
	/**
	 * Get the basic EU consume per stimulation.
	 */
	public double getEUConsume() {
		return 2718;
	}
	
	/**
	 * Get the basic EXP consume per stimulation.
	 */
	public int getExpConsume() {
		return 18;
	}
	
	public double getSuccessProb(AbilityData data) {
		return getSuccessfulProb(action, data);
	}
	
	public double getSuccessfulProb(IDevAction action, AbilityData data) {
		return getSyncRate() * action.getSuccessfulRate(data);
	}
	
	public boolean isStimSuccessful() {
		return this.stimSuccess == this.maxStimTimes;
	}
	
	public Pair<Integer, Double> getExpectation(IDevAction act, AbilityData data) {
		double prob = getSuccessfulProb(act, data);
		int times = act.getExpectedStims(data);
		return new Pair<Integer, Double>((int) (times * getExpConsume() / prob), times * getEUConsume() / prob);
	}
	
	/**
	 * Start stimulating at server with the given action ID.
	 */
	public void startStimulating(int id, int par) {
		if(user == null) {
			throw new RuntimeException("Developing without user");
		}
		if(id == -1) {
			isStimulating = false;
			return;
		}
		action = getAction(id, par);
		AbilityData data = AbilityDataMain.getData(user);
		isStimulating = true;
		maxStimTimes = action.getExpectedStims(data);
		stimSuccess = stimFailure = 0;
		sync(); //Force update if server
	}
	
	public float getSyncRateForDisplay() {
		return (float)getSyncRate() * 100F;
	}
	
	public double getSyncRate() {
		return 0.421 + nMagIncr * 0.12;
	}
	
	//Internal update
	@Override
	@SideOnly(Side.CLIENT)
	public void updateEntity() {
		super.updateEntity();
		if(!isHead())
			return;
		
		if(!worldObj.isRemote && this.es == null) {
			worldObj.spawnEntityInWorld(es 
				= new EntitySittable(worldObj, xCoord + .5F, yCoord + .6F, zCoord + .5F, xCoord, yCoord, zCoord));
		}
		
		boolean update = false;
		if(++updateCount >= UPDATE_RATE) {
			updateCount = 0;
			update = true;
		}
		
		if(!worldObj.isRemote && update) {
			updateModules();
		}
		
		if(!worldObj.isRemote && user != null) {
			//HeartBeat update
			if(isStimulating) {
				updateStimulate();
			}
			
			if(update) {
				sync();
			}
		}
	}
	
	private void updateModules() {
		final double r = MODULE_SEARCH_RANGE;
		AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(xCoord - r, yCoord - r, zCoord - r, xCoord + r, yCoord + r, zCoord + r);
		Set<BlockPos> set = GenericUtils.getBlocksWithinAABB(worldObj, bb, new IBlockFilter() {
			@Override
			public boolean accepts(World world, Block block, int x, int y, int z) {
				return block == ACBlocks.magInducer;
			}
		});
		this.nMagIncr = set.size();
	}
	
	private void sync() {
		if(!worldObj.isRemote) {
			AcademyCraft.netHandler.sendTo(new MsgDeveloper(this), (EntityPlayerMP) user);
		}
	}
	
	private TileDeveloper getHead() {
		Block b = getBlockType();
		if(!(b instanceof BlockDeveloper))
			return this;
		BlockDeveloper bd = (BlockDeveloper) b;
		TileEntity res = bd.getOriginTileEntity(getWorldObj(), xCoord, yCoord, zCoord, getBlockMetadata());
		if(res == null || !(res instanceof TileDeveloper)) {
			AcademyCraft.log.error("Didn't find the corresponding head for developer at " 
					+ DebugUtils.formatArray(xCoord, yCoord, zCoord) + " " + worldObj.isRemote);
			return this;
		}
		return (TileDeveloper) res;
	}
	
	private boolean isHead() {
		return getBlockMetadata() >> 2 == 0;
	}
	
	//Energy
	public double getMaxEnergy() {
		return INIT_MAX_ENERGY;
	}

	@Override
	public double demandedEnergyUnits() {
		return getHead().realDEU();
	}
	
	private double realDEU() {
		return super.demandedEnergyUnits();
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		return getHead().realIEU(directionFrom, amount);
	}
	
	private double realIEU(ForgeDirection directionFrom, double amount) {
		return super.injectEnergyUnits(directionFrom, amount);
	}
	
	//Registry
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderDeveloper renderer;
	
    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
    	@SideOnly(Side.CLIENT)
    	protected Object getClientContainer(EntityPlayer player, World world, int x, int y, int z) {
			TileEntity te = world.getTileEntity(x, y, z);
			if(te == null || !(te instanceof TileDeveloper)) {
				AcademyCraft.log.error("Failed opening developer gui: no TileDeveloper found");
				return null;
			}
			return new GuiDeveloper((TileDeveloper) te);
    	}
    	
    	protected Object getServerContainer(EntityPlayer player, World world, int x, int y, int z) {
    		return null;
    	}
    };
}
