/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderMdRayBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
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
	
	int rate;
	
	int workX, workY, workZ; //current aiming block
	int workTime = 0; //how long working on that block

	public EntityMiningRayBase(AbilityData data, SkillBase skill) {
		super(data.getPlayer());
		rate = getDigRate(data.getSkillLevel(skill), data.getLevelID() + 1);
	}

	public EntityMiningRayBase(World world) {
		super(world);
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
					workTime = 0;
					workX = nx;
					workY = ny;
					workZ = nz;
				}
				Block targ = worldObj.getBlock(workX, workY, workZ);
				int harvLevel = targ.getHarvestLevel(worldObj.getBlockMetadata(workX, workY, workZ));
				if(targ.getBlockHardness(worldObj, workX, workY, workZ) < 0|| harvLevel > getHarvestLevel()) {
					workX = workY = workZ = -1;
				}
			} else {
				workX = workY = workZ = -1;
			}
			
			if(workY != -1) {
				if(++workTime >= rate) {
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
	}
	
	protected abstract int getDigRate(int slv, int lv);
	
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
	}
	
}
