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
package cn.academy.vanilla.meltdowner.skill;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.apache.commons.lang3.tuple.Pair;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.vanilla.generic.entity.EntityRippleMark;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.raytrace.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class JetEngine extends Skill {

	public JetEngine() {
		super("jet_engine", 4);
	}
	
	@Override
	public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstance().addChild(new JEAction());
	}
	
	public static class JEAction extends SkillSyncAction {

		public JEAction() {
			super(-1);
		}
		
		@Override
		public void onStart() {
			super.onStart();
			
			if(isRemote)
				startEffects();
		}
		
		@Override
		public void onTick() {
			if(isRemote)
				updateEffects();
		}
		
		@Override
		public void onEnd() {
			
		}
		
		@Override
		public void onFinalize() {
			if(isRemote)
				endEffects();
		}
		
		Vec3 getDest() {
			double dist = 20.0;
			MovingObjectPosition result = Raytrace.traceLiving(player, dist, EntitySelectors.nothing);
			return result == null ? new Motion3D(player, true).move(dist).getPosVec() : result.hitVec;
		}
		
		Pair< Vec3, List<Entity> > getResult() {
			return null;
		}
		
		// CLIENT
		@SideOnly(Side.CLIENT)
		EntityRippleMark mark;
		
		@SideOnly(Side.CLIENT)
		void startEffects() {
			if(isLocal()) {
				world.spawnEntityInWorld(mark = new EntityRippleMark(world));
				mark.color.setColor4d(0.2, 1.0, 0.2, 0.7);
			}
		}
		
		@SideOnly(Side.CLIENT)
		void updateEffects() {
			if(isLocal()) {
				Vec3 dest = getDest();
				mark.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
			}
		}
		
		@SideOnly(Side.CLIENT)
		void endEffects() {
			if(isLocal()) {
				mark.setDead();
			}
		}
		
	}

}
