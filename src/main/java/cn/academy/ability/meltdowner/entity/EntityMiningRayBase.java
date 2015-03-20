/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.meltdowner.client.render.RenderMdRayBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public abstract class EntityMiningRayBase extends EntityMdRayBase {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static MiningRayRender render;
	
	float speed;
	int workX, workY, workZ; //current aiming block
	float targHardness;
	float curHardness;

	public EntityMiningRayBase(AbilityData data, SkillBase skill) {
		super(data.getPlayer());
		speed = getSpeed(data.getSkillLevel(skill), data.getLevelID() + 1);
	}

	public EntityMiningRayBase(World world) {
		super(world);
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(13, Integer.valueOf(-1));
		dataWatcher.addObject(14, Integer.valueOf(-1));
		dataWatcher.addObject(15, Integer.valueOf(-1));
		dataWatcher.addObject(16, Float.valueOf(0));
		dataWatcher.addObject(17, Float.valueOf(0));
		dataWatcher.addObject(18, Float.valueOf(0));
	}

	@Override
	public boolean doesFollowSpawner() {
		return true;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(!worldObj.isRemote) {
			MovingObjectPosition mop = GenericUtils.tracePlayer(getSpawner(), 12.0);
			if(mop != null) {
				int nx = mop.blockX, ny = mop.blockY, nz = mop.blockZ;
				if(nx != workX || ny != workY || nz != workZ) {
					targHardness = worldObj.getBlock(nx, ny, nz)
							.getBlockHardness(worldObj, nx, ny, nz);
					if(targHardness >= 0) {
						curHardness = 0;
						workX = nx;
						workY = ny;
						workZ = nz;
					}
				}
				Block targ = worldObj.getBlock(workX, workY, workZ);
				int harvLevel = targ.getHarvestLevel(worldObj.getBlockMetadata(workX, workY, workZ));
				if(harvLevel > getHarvestLevel()) {
					workX = workY = workZ = -1;
				}
			} else {
				workX = workY = workZ = -1;
			}
		}
		
		if(workY != -1) {
			curHardness += speed;
			if(!worldObj.isRemote) {
				if(curHardness >= targHardness) {
					//break the block
					Block block = worldObj.getBlock(workX, workY, workZ);
					int meta = worldObj.getBlockMetadata(workX, workY, workZ);
					onDiggedBlock(block, workX, workY, workZ, meta);
					worldObj.setBlockToAir(workX, workY, workZ);
					worldObj.playSoundEffect(workX + .5, workY + .5, workZ + .5, block.stepSound.getBreakSound(), .5f, 1f);
					workX = workY = workZ = -1; //Finished, invalidate
				}
			}
		}
		
		//Sync
		if(worldObj.isRemote) {
			workX = dataWatcher.getWatchableObjectInt(13);
			workY = dataWatcher.getWatchableObjectInt(14);
			workZ = dataWatcher.getWatchableObjectInt(15);
			speed = dataWatcher.getWatchableObjectFloat(16);
			curHardness = dataWatcher.getWatchableObjectFloat(17);
			targHardness = dataWatcher.getWatchableObjectFloat(18);
		} else {
			dataWatcher.updateObject(13, Integer.valueOf(workX));
			dataWatcher.updateObject(14, Integer.valueOf(workY));
			dataWatcher.updateObject(15, Integer.valueOf(workZ));
			dataWatcher.updateObject(16, Float.valueOf(speed));
			dataWatcher.updateObject(17, Float.valueOf(curHardness));
			dataWatcher.updateObject(18, Float.valueOf(targHardness));
		}
	}
	
	protected abstract float getSpeed(int slv, int lv);
	
	protected abstract int getHarvestLevel();
	
	protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
		 b.dropBlockAsItemWithChance(this.worldObj, 
					x, y, z, 
					meta, 1.0F, 0);
	}
	
	@Override
	public abstract ResourceLocation[] getTexData();
	
	@SideOnly(Side.CLIENT)
	public abstract float getRayWidth();
	
	@Override
	protected boolean attackOnSpawn() {
		return false;
	}
	
	public static class MiningRayRender extends RenderMdRayBase<EntityMiningRayBase> {
		
		@Override
		protected void drawAtOrigin(EntityMiningRayBase mrb, double len, boolean fp) {
			this.widthFp = mrb.getRayWidth();
			this.widthTp = widthFp * 2;
			super.drawAtOrigin(mrb, len, fp);
		}
		
		@Override
		public void doRender(Entity var1, double x, double y, double z, float h, float a) {
			super.doRender(var1, x, y, z, h, a);
			renderTarget((EntityMiningRayBase) var1);
		}
		
		private void renderTarget(EntityMiningRayBase ray) {
			System.out.println("Rendering targ");
			if(ray.targHardness != 0 && ray.workY != -1 && ray.worldObj.getBlock(ray.workX, ray.workY, ray.workZ).isOpaqueCube()) {
				GL11.glPushMatrix();
				
				double alpha = .8 * ray.curHardness / ray.targHardness;
				GL11.glColor4d(1, 1, 1, alpha);
				RenderUtils.loadTexture(ACClientProps.TEX_EFF_LAVA);
				double padding = 0.01;
				double x = ray.workX - padding - RenderManager.renderPosX,
					y = ray.workY - padding - RenderManager.renderPosY,
					z = ray.workZ - padding - RenderManager.renderPosZ;
				
				GL11.glTranslated(x, y, z);
				double len = padding * 2 + 1;
				RenderUtils.drawCube(len, len, len, true);
				
				GL11.glPopMatrix();
			}
		}
	}
	
}
