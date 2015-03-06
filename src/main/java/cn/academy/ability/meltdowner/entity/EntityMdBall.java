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
package cn.academy.ability.meltdowner.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderMdBall;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.FollowEntity;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Basic ball class. Implemented a very simple ticked event system.
 * It will automatically follow the player and float around.</br>
 * For better render effects, this class used hacking on positions. Use getPosition() to get real coordinates.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(freq = 2)
@RegEntity.HasRender
public class EntityMdBall extends EntityX {
	
	final ResourceLocation[] texs = ACClientProps.ANIM_MDBALL;
	
	public boolean load = false;
	public EntityPlayer spawner;
	public double offx, offy, offz;
	
	final int timeOffset = rand.nextInt(233333);
	
	protected int fadeTime = 10;
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMdBall render;
	
	int texID = 0;
	
	public EntityMdBall(EntityPlayer player) {
		super(player.worldObj);
		spawner = player;
		offx = (rand.nextBoolean() ? 1 : -1) * GenericUtils.randIntv(0.5, 0.8);
		offy = GenericUtils.randIntv(1.2, 2);
		offz = (rand.nextBoolean() ? 1 : -1) * GenericUtils.randIntv(0.5, 0.8);
		addDaemonHandler(new FollowEntity(this, player).setOffset(
				offx, offy, offz));
		setPosition(player.posX + offx, player.posY + offy, player.posZ + offz);
		addDaemonHandler(new VelocityUpdate(this, 1));
	}

	public EntityMdBall(World world) {
		super(world);
		addDaemonHandler(new VelocityUpdate(this, 1));
		ignoreFrustumCheck = true;
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
		dataWatcher.addObject(11, Integer.valueOf(0));
		dataWatcher.addObject(12, Float.valueOf(0));
		dataWatcher.addObject(13, Float.valueOf(0));
		dataWatcher.addObject(14, Float.valueOf(0));
	}
	
	@Override
	public void onUpdate() {
		if(spawner != null && doesFollow()) {
			motionX = spawner.motionX;
			motionY = spawner.motionY;
			motionZ = spawner.motionZ;
		}
		
		super.onUpdate();
		
		texID = rand.nextInt(texs.length);
		sync();
	}
	
	private void sync() {
		if(!worldObj.isRemote) {
			dataWatcher.updateObject(11, Integer.valueOf(spawner.getEntityId()));
			
			dataWatcher.updateObject(12, Float.valueOf((float) offx));
			dataWatcher.updateObject(13, Float.valueOf((float) offy));
			dataWatcher.updateObject(14, Float.valueOf((float) offz));
		} else {
			Entity ent = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(11));
			offx = dataWatcher.getWatchableObjectFloat(12);
			offy = dataWatcher.getWatchableObjectFloat(13);
			offz = dataWatcher.getWatchableObjectFloat(14);
			if(ent instanceof EntityPlayer && !load) {
				load = true;
				spawner = (EntityPlayer) ent;
			}
		}
	}
	
	/**
	 * Get real position for this md ball.
	 */
	public double[] getPosition() {
		if(spawner == null) {
			return new double[] { posX, posY, posZ };
		}
		return new double[] {
			spawner.posX + offx,
			spawner.posY + (worldObj.isRemote ? 0 : 1.6) + offy,
			spawner.posZ + offz
		};
	}
	
	public ResourceLocation getTexture() {
		return texs[texID % texs.length];
	}
	
    @Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
    	setDead();
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	public boolean doesFollow() {
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	public double getAlpha() {
		return Math.min(1.0, (double)ticksExisted / fadeTime);
	}

}
