/**
 * 
 */
package cn.academy.ability.electro.skill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.entity.EntityBlockSimulator;
import cn.academy.misc.util.JudgeUtils;
import cn.liutils.api.entityx.FakeEntity;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.BlockPos;
import cn.liutils.util.space.IBlockFilter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Make the player able to see the mineral block information through a surrounding region for a specified len of time.
 * @author WeathFolD
 */
public class SkillMineDetect extends SkillBase {

	private static IBlockFilter blockFilter = new IBlockFilter() {
		@Override
		public boolean accepts(World world, Block block, int x, int y, int z) {
			return JudgeUtils.isOreBlock(block);
		}
	};
	

	public SkillMineDetect() {
		setLogo("electro/mineview.png");
		setName("em_mine");
		setMaxSkillLevel(10);
	}
	
	static int getCPConsume(int slv, int lv) {
		return 3000 + slv * 50 + lv * 200;
	}
	
	static double getDetectRange(int slv, int lv) {
		return 10 + slv * 2 + lv * 5;
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public SkillState createSkill(EntityPlayer player) {
				return new EnableVision(player);
			}
			
		});
	}
	
	@SideOnly(Side.CLIENT)
	private static ResourceLocation getTexture(Block block) {
		return ACClientProps.EFF_MV_TEST;
	}
	
	@SideOnly(Side.CLIENT)
	public static final class HandlerEntity extends FakeEntity {
		
		//Serve as a pool.
		final List<EntityBlockSimulator> aliveSims = new ArrayList<EntityBlockSimulator>();
		
		final int lifeTime;
		public final double range;
		private double safeDistSq; //How many blocks until we peform an update
		
		private double lastX, lastY, lastZ;
		
		private EntityPlayer target;
		
		private final boolean isAdvanced;

		public HandlerEntity(EntityPlayer target, int time, double range, boolean advanced) {
			super(target);
			this.lifeTime = time;
			this.range = range;
			this.target = target;
			
			double tmp = range * 0.2;
			safeDistSq = tmp * tmp;
			
			isAdvanced = advanced;
			
			target.addPotionEffect(new PotionEffect(Potion.blindness.id, 10000));
			this.setCurMotion(new Ticker());
		}
		
		private class Ticker extends MotionHandler {
			
			int ticksUntilUpdate = 0;

			public Ticker() {
				super(HandlerEntity.this);
			}

			@Override
			public void onCreated() {
				updateBlocks();
			}

			@Override
			public void onUpdate() {
				
				double distSq = GenericUtils.distanceSq(posX, posY, posZ, lastX, lastY, lastZ);
				if(distSq > safeDistSq) {
					updateBlocks();
				}
				
				if(ticksExisted == lifeTime) {
					for(EntityBlockSimulator ebs : aliveSims) {
						ebs.setDead();
					}
					aliveSims.clear();
					setDead();
					target.removePotionEffect(Potion.blindness.id);
				}
			}
			
			private void updateBlocks() {
				Iterator<EntityBlockSimulator> iter = aliveSims.iterator();
				while(iter.hasNext()) {
					EntityBlockSimulator ebs = iter.next();
					if(ebs.isDead) iter.remove();
				}
				
				AxisAlignedBB aabb = AxisAlignedBB.getAABBPool().getAABB(
						posX - range * 0.5, posY - range * 0.5, posZ - range * 0.5,
						posX + range * 0.5, posY + range * 0.5, posZ + range * 0.5);
				Set<BlockPos> set = GenericUtils.getBlocksWithinAABB(worldObj, aabb, blockFilter);
				int ind = 0;
					
				Set<EntityBlockSimulator> toRetain = new HashSet<EntityBlockSimulator>();
				for(BlockPos bp : set) {
					//Get a new EBS and set
					EntityBlockSimulator ebs;
					if(ind < aliveSims.size()) {
						ebs = aliveSims.get(ind);
						ind += 1;
					} else {
						ebs = new EntityBlockSimulator(HandlerEntity.this, bp, isAdvanced);
						worldObj.spawnEntityInWorld(ebs);
					}
					toRetain.add(ebs);
				}
				
				//Clear the rest(which are useless)
				for(int i = ind; i < aliveSims.size(); ++i) {
					aliveSims.get(i).setDead();
				}
				aliveSims.clear();
				
				aliveSims.addAll(toRetain);
				
				lastX = posX;
				lastY = posY;
				lastZ = posZ;
			}

			@Override
			public String getID() {
				return "baka";
			}
			
		}
		
	}
	
	public static final class EnableVision extends SkillState {

		public EnableVision(EntityPlayer player) {
			super(player);
		}
		
		@Override
		protected void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatElectro.mineDetect), lv = data.getLevelID() + 1;
			if(!data.decreaseCP(getCPConsume(slv, lv), CatElectro.mineDetect)) {
				return;
			}
			player.playSound("academy:elec.mineview", 0.5f, 1.0f);
			if(player.worldObj.isRemote) {
				player.worldObj.spawnEntityInWorld(
					new HandlerEntity(player, 100, getDetectRange(slv, lv), lv >= 3 && slv >= 5));
			}
			this.finishSkill();
		}
		
	}

}
