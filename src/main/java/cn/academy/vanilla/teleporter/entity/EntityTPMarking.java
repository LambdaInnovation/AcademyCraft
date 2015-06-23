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
package cn.academy.vanilla.teleporter.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import cn.academy.ability.api.AbilityData;
import cn.academy.vanilla.teleporter.client.MarkRender;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Spawn a position mark indicating where the player would be teleport to.
 * You should spawn this entity in both sides and it will not synchronize.
 * @author WeathFolD
 */
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public abstract class EntityTPMarking extends EntityAdvanced {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static MarkRender render;
	
	static TPParticleFactory particleFac = TPParticleFactory.instance;
	
	final AbilityData data;
	protected final EntityPlayer player;

	public EntityTPMarking(EntityPlayer player) {
		super(player.worldObj);
		data = AbilityData.get(player);
		this.player = player;
		updatePos();
		//this.ignoreFrustumCheck = true;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		rotationPitch = player.rotationPitch;
		rotationYaw = player.rotationYaw;
		this.updatePos();
		
		particleFac.setPosition(
			posX + RandUtils.ranged(-1, 1), 
			posY + RandUtils.ranged(0, 1), 
			posZ + RandUtils.ranged(-1, 1));
		particleFac.setVelocity(
			RandUtils.ranged(-.05, .05),
			RandUtils.ranged(0, 0.05),
			RandUtils.ranged(-.05, .05));
		
		worldObj.spawnEntityInWorld(particleFac.next(worldObj));
	}
	
	public static void sync(@Target EntityPlayer player) {
		
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	protected void updatePos() {
		double md = getMaxDistance();
		MovingObjectPosition mop = Raytrace.traceLiving(player, md);
		
		
		if(mop != null) {
			double x = mop.hitVec.xCoord,
					y= mop.hitVec.yCoord,
					z= mop.hitVec.zCoord;
			switch(mop.sideHit) {
			case 0:
				y -= 1.0; break;
			case 1:
				y += 1.8; break;
			case 2:
				z -= .6; y = mop.blockY + 1.7; break;
			case 3:
				z += .6; y = mop.blockY + 1.7;  break;
			case 4:
				x -= .6; y = mop.blockY + 1.7;  break;
			case 5: 
				x += .6; y = mop.blockY + 1.7;  break;
			}
			//check head
			if(mop.sideHit > 1) {
				int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
				if(!worldObj.isAirBlock(hx, hy, hz)) {
					y -= 1.25;
				}
			}
			
			setPosition(x, y, z);
		} else {
			Motion3D mo = new Motion3D(player, true);
			mo.move(md);
			setPosition(mo.px, mo.py, mo.pz);
		}
	}
	
	public double getDist() {
		return this.getDistanceToEntity(player);
	}
	
	protected abstract double getMaxDistance();
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {}

}