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
import cn.academy.ability.api.data.AbilityData;
import cn.academy.vanilla.teleporter.client.MarkRender;
import cn.academy.vanilla.teleporter.client.TPParticleFactory;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Spawn a position mark indicating where the player would be teleport to.
 * You should spawn it in CLIENT ONLY.
 * @author WeathFolD
 */
@Registrant
@RegEntity(clientOnly = true)
@SideOnly(Side.CLIENT)
@RegEntity.HasRender
public class EntityTPMarking extends EntityAdvanced {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static MarkRender render;
	
	static TPParticleFactory particleFac = TPParticleFactory.instance;
	
	final AbilityData data;
	protected final EntityPlayer player;
	
	public boolean available = true;

	public EntityTPMarking(EntityPlayer player) {
		super(player.worldObj);
		data = AbilityData.get(player);
		this.player = player;
		setPosition(player.posX, player.posY, player.posZ);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		rotationPitch = player.rotationPitch;
		rotationYaw = player.rotationYaw;
		
		if(available && rand.nextDouble() < 0.4) {
			particleFac.setPosition(
				posX + RandUtils.ranged(-1, 1), 
				posY + RandUtils.ranged(0.2, 1.6) - 1.6, 
				posZ + RandUtils.ranged(-1, 1));
			particleFac.setVelocity(
				RandUtils.ranged(-.03, .03),
				RandUtils.ranged(0, 0.05),
				RandUtils.ranged(-.03, .03));
			
			worldObj.spawnEntityInWorld(particleFac.next(worldObj));
		}
	}
	
	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}
	
	public double getDist() {
		return this.getDistanceToEntity(player);
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {}

}