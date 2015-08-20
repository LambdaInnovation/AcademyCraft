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
import cn.academy.ability.client.render.RenderDeveloper;
import cn.academy.ability.developer.Developer;
import cn.academy.ability.developer.DeveloperBlock;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.AcademyCraft;
import cn.academy.core.block.TileReceiverBase;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegTileEntity;
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
@RegTileEntity
@RegTileEntity.HasRender
public abstract class TileDeveloper extends TileReceiverBase {
	
	@RegTileEntity
	public static class Normal extends TileDeveloper {

		@Override
		public DeveloperType getType() {
			return DeveloperType.NORMAL;
		}
		
	}
	
	@RegTileEntity
	public static class Advanced extends TileDeveloper {

		@Override
		public DeveloperType getType() {
			return DeveloperType.ADVANCED;
		}
		
	}
	
	@SideOnly(Side.CLIENT)
	@RegTileEntity.Render
	public static RenderDeveloper renderer;
	
	public DeveloperBlock developer;
	
	static InstanceSerializer<EntityPlayer> ser = 
		SerializationManager.INSTANCE.getInstanceSerializer(EntityPlayer.class);

	@SideOnly(Side.CLIENT)
	public boolean isDevelopingDisplay;
	@SideOnly(Side.CLIENT)
	public double devProgressDisplay;
	
	private static ScriptNamespace script;
	
	public static void init() {
		script = AcademyCraft.getScript().at("ac.developer");
	}
	
	int syncCD;
	
	EntityPlayer user;
	
	public TileDeveloper() {
		super("ability_developer", 2, 100000, 300);
	}

	@Override
	public void updateEntity() {
		if(developer == null)
			developer = new DeveloperBlock(this);
		
		developer.tick();
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
	
	private String propPath(String val) {
		return getType().toString().toLowerCase() + "." + val;
	}
	
	public abstract DeveloperType getType();
	
}
