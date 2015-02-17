package cn.academy.ability.teleport.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.ability.teleport.entity.EntityTPMarking;
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
	}
	
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
		return 30 + slv * 8 + lv * 20;
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
			mark = new EntityTPMarking(player) {

				@Override
				protected double getMaxDistance() {
					int slv = data.getSkillLevel(CatTeleport.skillMarkedTele), lv = data.getLevelID() + 1;
					
					return Math.min(
						Math.min(3 * ticksExisted, 13 + slv * .5 + lv * 2.5), 
						SkillMarkTele.this.getMaxDistance(
						data.getCurrentCP(), slv, lv)
					);
				}
				
			};
			player.worldObj.spawnEntityInWorld(mark);
		}

		@Override
		public void onFinish() {
			//player.motionX = player.motionZ = player.motionY = 0;
			double dist = mark.getDist();
			//Here we ignore the slight variation and believe that we will always success
			if(player instanceof EntityPlayerMP) {
				((EntityPlayerMP)player).setPositionAndUpdate(mark.posX, mark.posY, mark.posZ);
			} else {
				player.setPosition(mark.posX, mark.posY, mark.posZ);
			}
			player.fallDistance = 0.0f;
			player.playSound("academy:tp.tp", 1.0f, 1.0f);
			
			data.decreaseCP((float) 
				(dist * getConsumePerBlock(
				data.getSkillLevel(CatTeleport.skillMarkedTele), 
				data.getLevelID() + 1)), true);
			mark.setDead();
		}

		@Override
		public void onHold() {}
		
	}
}
