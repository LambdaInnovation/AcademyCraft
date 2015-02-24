/**
 * 
 */
package cn.academy.ability.electro.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.CatElectro;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.util.JudgeUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.FollowEntity;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.space.BlockPos;
import cn.liutils.util.space.IBlockFilter;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Make the player able to see the mineral block information through a surrounding region for a specified len of time.
 * @author WeathFolD
 */
@RegistrationClass
public class SkillMineDetect extends SkillBase {
	
	static final int colors[][] = { //alpha will be reset each time rendering
		{115, 200, 227, 0}, //default color
		{161, 181, 188, 0}, //harvest level 0-3
		{87, 231, 248, 0},
		{97, 204, 94, 0},
		{235, 109, 84, 0}
	};

	private static IBlockFilter blockFilter = new IBlockFilter() {
		@Override
		public boolean accepts(World world, Block block, int x, int y, int z) {
			return JudgeUtils.isOreBlock(block);
		}
	};
	

	public SkillMineDetect() {
		setLogo("electro/mineview.png");
		setName("em_mine");
		setMaxLevel(10);
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
		return ACClientProps.EFF_MV;
	}
	
	public static class MineElem {
		public final int x, y, z, level;
		public MineElem(int _x, int _y, int _z, int _lv) {
			x = _x;
			y = _y;
			z = _z;
			level = _lv; //this correspond to the color array index
		}
	}
	
	@SideOnly(Side.CLIENT)
	@RegEntity(clientOnly = true)
	@RegEntity.HasRender
	public static final class HandlerEntity extends EntityX {
		
		@RegEntity.Render
		public static HandlerRender renderer;
		
		//Current elements to display.
		final List<MineElem> aliveSims = new ArrayList<MineElem>();
		
		final int lifeTime;
		public final double range;
		private double safeDistSq; //How many blocks until we peform an update
		
		private double lastX, lastY, lastZ;
		
		private EntityPlayer target;
		
		private final boolean isAdvanced;

		public HandlerEntity(EntityPlayer target, int time, double range, boolean advanced) {
			super(target.worldObj);
			
			this.ignoreFrustumCheck = true;
			
			this.lifeTime = time;
			this.range = Math.min(range, 28);
			this.target = target;
			
			double tmp = range * 0.2;
			safeDistSq = tmp * tmp;
			
			isAdvanced = advanced;
			
			target.addPotionEffect(new PotionEffect(Potion.blindness.id, time));
			
			setPosition(target.posX, target.posY, target.posZ);
			addDaemonHandler(new FollowEntity(this, target));
			setCurMotion(new Ticker());
		}
		
		@Override
		public boolean shouldRenderInPass(int pass) {
			return pass == 1;
		}
		
		private class Ticker extends MotionHandler {

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
				
				if(ticksExisted > lifeTime) {
					setDead();
				}
			}
			
			private void updateBlocks() {
				aliveSims.clear();
				
				AxisAlignedBB aabb = AxisAlignedBB.getAABBPool().getAABB(
						posX - range * 0.5, posY - range * 0.5, posZ - range * 0.5,
						posX + range * 0.5, posY + range * 0.5, posZ + range * 0.5);
				Set<BlockPos> set = GenericUtils.getBlocksWithinAABB(worldObj, aabb, blockFilter);
					
				for(BlockPos bp : set) {
					aliveSims.add(new MineElem(
						bp.x, bp.y, bp.z, 
						isAdvanced ? Math.min(3, (bp.block.getHarvestLevel(0) + 1)) : 0));
				}
				
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
	
	@SideOnly(Side.CLIENT)
	public static class HandlerRender extends Render {

		@Override
		public void doRender(Entity var1, double var2, double var4,
				double var6, float var8, float var9) {
			HandlerEntity he = (HandlerEntity) var1;
			for(MineElem me : he.aliveSims) {
				drawSingle(me, (int) 
					(calcAlpha(he.posX - me.x, he.posY - me.y, he.posZ - me.z, he.range) * 255));
			}
		}
		
		private float calcAlpha(double x, double y, double z, double range) {
			double jdg = 1 - GenericUtils.distance(x, y, z) / range * 2.2;
			return 0.3f + (float) (jdg * 0.7);
		}
		
		private void drawSingle(MineElem me, int alpha) {
			double x = me.x - RenderManager.renderPosX, 
				y = me.y - RenderManager.renderPosY, 
				z = me.z - RenderManager.renderPosZ;
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_FOG);
			GL11.glPushMatrix(); {
				RenderUtils.loadTexture(ACClientProps.EFF_MV);
				GL11.glTranslated(x + .05, y + .05, z + .05);
				int[] color = colors[me.level];
				color[3] = alpha;
				RenderUtils.bindColor(color);
				RenderUtils.drawCube(.9, .9, .9, true);
			} GL11.glPopMatrix();
			GL11.glEnable(GL11.GL_FOG);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_LIGHTING);
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
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
