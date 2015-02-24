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
package cn.academy.ability.meltdowner.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.client.render.RenderMiningRay;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.motion.LifeTime;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityMiningRay extends EntityMdRayBase {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RenderMiningRay render;
	
	int harvestLevel;
	
	public EntityMiningRay(EntityPlayer _spawner, EntityMdBall _ball, int _hlv) {
		super(_spawner, _ball);
		harvestLevel = _hlv;
		this.addDaemonHandler(new LifeTime(this, 22));
	}

	@SideOnly(Side.CLIENT)
	public EntityMiningRay(World world) {
		super(world);
	}
	
	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(getSpawner()), 2);
		} else {
			if(!worldObj.isRemote) {
				Block block = worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ);
				int meta = worldObj.getBlockMetadata(mop.blockX, mop.blockY, mop.blockZ);
				if(harvestLevel >= block.getHarvestLevel(meta)) {
					onDiggedBlock(block, mop.blockX, mop.blockY, mop.blockZ, meta);
					worldObj.setBlockToAir(mop.blockX, mop.blockY, mop.blockZ);
				}
			}
		}
	}
	
	protected void onDiggedBlock(Block b, int x, int y, int z, int meta) {
		 b.dropBlockAsItemWithChance(this.worldObj, 
					x, y, z, 
					meta, 1.0F, 0);
	}
	
	@Override
	public boolean isNearPlayer() {
		return false;
	}
	
	@Override
	public double getAlpha() {
		return ticksExisted > 15 ? Math.max(0, 1 - (double) (ticksExisted - 15) / (7)) : 1;
	}

	@Override
	public ResourceLocation[] getTexData() {
		return ACClientProps.ANIM_MD_RAY_S;
	}


	
}
