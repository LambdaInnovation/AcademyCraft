/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.ability.developer.Developer;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.AcademyCraft;
import cn.academy.core.block.TileReceiverBase;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.SerializationManager;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.liutils.ripple.ScriptNamespace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
@RegInit
public class TileDeveloper extends TileReceiverBase {
	
	public final Developer developer;
	
	static InstanceSerializer<EntityPlayer> ser = SerializationManager.INSTANCE.getInstanceSerializer(EntityPlayer.class);

	@SideOnly(Side.CLIENT)
	public boolean isDevelopingDisplay;
	@SideOnly(Side.CLIENT)
	public double devProgressDisplay;
	
	private static ScriptNamespace script;
	
	public static void init() {
		script = AcademyCraft.script.at("ac.developer");
	}
	
	int syncCD;
	
	EntityPlayer user;
	
	public TileDeveloper() {
		super("ability_developer", 2, 100000, 300);
		developer = new Developer() {

			@Override
			public EntityPlayer getUser() {
				return user;
			}

			@Override
			public int getTPS() {
				return TileDeveloper.this.getTPS();
			}

			@Override
			public boolean pullEnergy(double amt) {
				return TileDeveloper.this.pullEnergy(amt) == amt;
			}

			@Override
			public double getCPS() {
				return getConsumePerStim();
			}
			
		};
	}

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			
			developer.tick();
			
		} else {
			
			// Sync faster when developing.
			// TODO: Better off syncing those in Container
			if(++syncCD == (isDevelopingDisplay ? 5 : 20)) {
				syncCD = 0;
				doSync();
			}
			
		}
	}
	
	public Developer getDeveloper() {
		return developer;
	}
	
	public EntityPlayer getUser() {
		return user;
	}
	
	public boolean use(EntityPlayer player) {
		if(user == null) {
			user = player;
			developer.reset();
			return true;
		}
		return player.equals(user);
	}
	
	public void unuse() {
		user = null;
		developer.reset();
	}
	
	/**
	 * Get the "Tick per stimulation" param.
	 */
	public int getTPS() {
		return script.getInteger(propPath("tps"));
	}
	
	/**
	 * TODO: Implement modules
	 */
	public double getConsumePerStim() {
		return 1000;
	}
	
	private String propPath(String val) {
		return getType().toString().toLowerCase() + "." + val;
	}
	
	private DeveloperType getType() {
		Block blockType = getBlockType();
		DeveloperType type = blockType instanceof BlockDeveloper ? ((BlockDeveloper)blockType).type : DeveloperType.PORTABLE;
		return type;
	}
	
	@SideOnly(Side.CLIENT)
	private void doSync() {
		Future future = Future.create((Object ret) -> {
			NBTTagCompound tag = (NBTTagCompound) ret;
			isDevelopingDisplay = tag.getBoolean("d");
			devProgressDisplay = tag.getDouble("p");
			
//			NBTBase tag2 = tag.getTag("a");
//			if(tag2 != null) {
//				user = ret.
//			}
		});
		sync(future);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	private void sync(@Data Future future) {
		NBTTagCompound ret = new NBTTagCompound();
		
		boolean dev;
		double devProg;
		
		if(developer == null) {
			dev = false;
			devProg = 0;
		} else {
			dev = developer.isDeveloping();
			devProg = developer.getDevelopProgress();
		}
		
		ret.setBoolean("d", dev);
		ret.setDouble("p", devProg);
		
//		if(getUser() != null) {
//			try {
//				ret.setTag("a", ser.writeInstance(getUser()));
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		
		future.setAndSync(ret);
	}
	
}
