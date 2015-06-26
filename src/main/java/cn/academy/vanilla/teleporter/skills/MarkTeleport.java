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
package cn.academy.vanilla.teleporter.skills;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.CPData;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.core.util.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class MarkTeleport extends Skill {
	
	private static MarkTeleport instance;
	
	private static double MINIMUM_VALID_DISTANCE = 3.0;

	public MarkTeleport() {
		super("mark_teleport", 2);
		instance = this;
	}
	
	public static double getMaxDist(AbilityData data, CPData cpData) {
		double max = 20.0;
		double 
			cplim = cpData.getCP() / getCPB(data), 
			olim = (2 * cpData.getMaxOverload() - cpData.getOverload()) / getOPB(data);
		
		return Math.min(max, Math.min(cplim, olim));
	}
	
	/**
	 * @return Consumption per block
	 */
	public static float getCPB(AbilityData data) {
		return 100.0f;
	}
	
	/**
	 * @return Overload per block
	 */
	public static float getOPB(AbilityData data) {
		return 1.0f;
	}
	
	private static Vec3 getDest(EntityPlayer player) {
		double dist = getMaxDist(AbilityData.get(player), CPData.get(player));
		
		MovingObjectPosition mop = Raytrace.traceLiving(player, dist);
		
		double x, y, z;
		
		if(mop != null) {
			x = mop.hitVec.xCoord;
			y= mop.hitVec.yCoord;
			z= mop.hitVec.zCoord;
			
			if(mop.typeOfHit == MovingObjectType.BLOCK) {
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
					if(!player.worldObj.isAirBlock(hx, hy, hz)) {
						y -= 1.25;
					}
				}
			} else {
				y += mop.entityHit.getEyeHeight();
			}
		} else {
			Motion3D mo = new Motion3D(player, true).move(dist);
			x = mo.px;
			y = mo.py;
			z = mo.pz;
		}
		
		return VecUtils.vec(x, y, z);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new MTAction());
	}
	
	public static class MTAction extends SyncAction {
		
		@SideOnly(Side.CLIENT)
		MTMark mark;
		
		AbilityData aData;
		CPData cpData;

		public MTAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			aData = AbilityData.get(player);
			cpData = CPData.get(player);
			
			if(isRemote) {
				startMark();
			}
		}
		
		@Override
		public void onEnd() {
			Vec3 dest = getDest(player);
			float distance = (float) dest.distanceTo(VecUtils.vec(player.posX, player.posY, player.posZ));
			if(distance < MINIMUM_VALID_DISTANCE) {
				// TODO: Play abort sound
				;
			} else {
			
				cpData.perform(distance * getOPB(aData), distance * getCPB(aData));
				
				if(!isRemote) {
					((EntityPlayerMP)player).setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
				} else {
					ACSounds.playClient(player, "tp.tp", .5f, RandUtils.rangef(0.8f, 1.2f));
				}
			}
			
			if(isRemote) {
				endMark();
			}
		}
		
		@Override
		public void onAbort() {
			if(isRemote) {
				endMark();
			}
		}
		
		@SideOnly(Side.CLIENT)
		private void startMark() {
			player.worldObj.spawnEntityInWorld(mark = new MTMark(player));
		}
		
		@SideOnly(Side.CLIENT)
		private void endMark() {
			mark.setDead();
		}
		
	}
	
	private static class MTMark extends EntityTPMarking {

		public MTMark(EntityPlayer player) {
			super(player);
		}

		@Override
		protected double getMaxDistance() {
			return getMaxDist(AbilityData.get(player), CPData.get(player));
		}
		
	}

}
