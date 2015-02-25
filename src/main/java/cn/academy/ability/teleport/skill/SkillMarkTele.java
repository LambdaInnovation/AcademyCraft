/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.ability.teleport.entity.fx.EntityTPMarking;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;

/**
 * Marked tele skill
 * @author WeathFolD
 */
public class SkillMarkTele extends SkillBase {
	
	public SkillMarkTele() {
		setName("tp_mark");
		setLogo("tp/marked.png");
		setMaxLevel(10);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(400) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new MTState(player);
			}
			
		});
	}
	
	private int getMaxDistance(float cp, int slv, int lv) {
		return (int) (cp / getConsumePerBlock(slv, lv));
	}
	
	private float getConsumePerBlock(int slv, int lv) {
		return 30 + slv * 6 + lv * 10;
	}
	
	private double getPosErrorRatio(int slv, int lv) {
		return 4 - slv * 0.1 - lv * 0.3;
	}
	
	public class MTState extends State {
		
		final AbilityData data;
		
		private EntityTPMarking mark;

		public MTState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
		}

		@Override
		public void onStart() {
			int slv = data.getSkillLevel(CatTeleport.skillMarkedTele), lv = data.getLevelID() + 1;
			double dist = SkillMarkTele.this.getMaxDistance(data.getCurrentCP(), slv, lv);
			if(dist < 1.5) { //Too near, can't teleport
				if(player.worldObj.isRemote) {
					player.playSound("academy:deny", .5f, 1f);
				}
				finishSkill();
				return;
			}
			
			
			mark = new EntityTPMarking(player) {

				@Override
				protected double getMaxDistance() {
					int slv = data.getSkillLevel(CatTeleport.skillMarkedTele), lv = data.getLevelID() + 1;
					double dist = SkillMarkTele.this.getMaxDistance(data.getCurrentCP(), slv, lv);
					return Math.min(
						Math.min(3 * ticksExisted, 13 + slv * .5 + lv * 2.5), 
						SkillMarkTele.this.getMaxDistance(
						data.getCurrentCP(), slv, lv)
					);
				}
				
			};
			player.playSound("academy:tp.tp_pre", 1F, 1.0F);
			player.worldObj.spawnEntityInWorld(mark);
		}

		@Override
		public void onFinish() {
			if(mark == null)
				return;
			
			//player.motionX = player.motionZ = player.motionY = 0;
			double dist = mark.getDist();
			//Here we ignore the slight variation and believe that we will always success
			if(player instanceof EntityPlayerMP) {
				((EntityPlayerMP)player).setPositionAndUpdate(mark.posX, mark.posY, mark.posZ);
			}
			player.fallDistance *= 0.3f; //lower the fall distance.
			player.worldObj.playSoundAtEntity(player, "academy:tp.tp", .5f, 1f);
			if(!isRemote())
			data.decreaseCP((float) 
				(dist * getConsumePerBlock(
				data.getSkillLevel(CatTeleport.skillMarkedTele), 
				data.getLevelID() + 1)), CatTeleport.skillMarkedTele, true);
			mark.setDead();
		}

		@Override
		public void onHold() {}
		
	}
}
