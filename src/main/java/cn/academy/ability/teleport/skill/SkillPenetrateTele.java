package cn.academy.ability.teleport.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.teleport.CatTeleport;
import cn.academy.ability.teleport.entity.fx.EntityTPMarking;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.util.space.Motion3D;

public class SkillPenetrateTele extends SkillBase {
	public SkillPenetrateTele() {
		setName("tp_penetrate");
		setLogo("tp/penetrate.png");
		setMaxSkillLevel(15);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new PeneState(player);
			}
			
		});
	}
	
	private static float getConsumePerBlock(int slv, int lv) {
		return 30 + slv * 8 + lv * 20;
	}
	
	private static float getMaxDistance(int slv, int lv) {
		return 5 + slv * 2 + lv * 5;
	}
	
	//TODO: Boilerplate
	public static class PeneState extends State {
		
		final AbilityData data;
		private EntityTPMarking mark;

		public PeneState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
		}

		@Override
		public void onStart() {
			mark = new PeneMarking(player);
			player.playSound("academy:tp.tp_pre", 0.5F, 1.0F);
			player.worldObj.spawnEntityInWorld(mark);
		}

		@Override
		public void onFinish() {
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
				data.getSkillLevel(CatTeleport.skillPenetrateTele), 
				data.getLevelID() + 1)), CatTeleport.skillPenetrateTele, true);
			mark.setDead();
		}

		@Override
		public void onHold() {}
		
	}
	
	private static class PeneMarking extends EntityTPMarking {
		public PeneMarking(EntityPlayer player) {
			super(player);
		}
		
		protected void updatePos() {
			Vec3 targ = getTargetPos(player, getMaxDistance());
			setPosition(targ.xCoord, targ.yCoord, targ.zCoord);
		}

		@Override
		protected double getMaxDistance() {
			AbilityData data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatTeleport.skillPenetrateTele), lv = data.getLevelID() + 1;
			return Math.min(SkillPenetrateTele.getMaxDistance(slv, lv), 
					data.getCurrentCP() / getConsumePerBlock(slv, lv));
		}
	}
	
	/**
	 * The position detect algorithm
	 * @param player
	 * @param maxDist
	 * @return
	 */
	private static Vec3 getTargetPos(EntityPlayer player, double maxDist) {
		World world = player.worldObj;
		Motion3D mo = new Motion3D(player, true).move(1.0d);
		double nearX = mo.posX, nearY = mo.posY, nearZ = mo.posZ;
		
		boolean nearDet = false,  //Whether we have determined the near point.
				meetd = false; //Whether we have met the first 'dangerous' point.
		System.out.println("---");
		for(int i = 0; i <= maxDist; ++i) {
			boolean safe;
			int cx = (int) mo.posX, cy = (int) mo.posY, cz = (int) mo.posZ;
			int ngood = 0;
			for(int y = cy - 1; y <= cy + 1; ++y) {
				if(world.isAirBlock(cx, y, cz)) {
					++ngood;
				}
			}
			safe = ngood >= 2;
			if(!meetd) { //in the safe range
				if(!safe) {
					meetd = true;
				}
			} else if(!nearDet) { //in the dangerous area
				if(safe) {
					nearDet = true;
					nearX = mo.posX; nearY = mo.posY; nearZ = mo.posZ;
				}
			} else { //now in the next safe area, try to go further
				if(!safe) break; //next dangerous point, end
			}
			mo.move(1.0d);
			System.out.println(i);
		}
		System.out.println("---");
		
		double farX = mo.posX, farY = mo.posY, farZ = mo.posZ;
		return world.getWorldVec3Pool().getVecFromPool((nearX + farX) / 2, (nearY + farY) / 2, (nearZ + farZ) / 2);
	}
}
