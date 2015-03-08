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
package cn.academy.ability.meltdowner.entity;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;
import cn.academy.misc.entity.EntitySilbarn;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.misc.IntRandomSequence;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity()
@RegEntity.HasRender
public class EntityMeltDowner extends EntityMdRayBase {
	
	IntRandomSequence seq = new IntRandomSequence(20, getTexData().length - 1);
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderMD renderer;
	
	float dmg;
	
	{
		this.execAfter(30, new EntityCallback<EntityMeltDowner>() {
			@Override
			public void execute(EntityMeltDowner ent) {
				ent.setFadeout(10);
			}
		});
	}

	public EntityMeltDowner(EntityPlayer _spawner, float dmg) {
		super(_spawner);
		this.dmg = dmg;
	}

	@SideOnly(Side.CLIENT)
	public EntityMeltDowner(World world) {
		super(world);
	}

	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			if(mop.entityHit instanceof EntitySilbarn) {
				doScatterAt((EntitySilbarn) mop.entityHit);
				return;
			} else {
				mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getSpawner()), dmg);
			}
		}
		//真男人从不回头看爆炸
		GenericUtils.explode(worldObj, getSpawner(), dmg * .2f, dmg * .15f, 
			mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, dmg * .5f);
	}
	
	/**
	 * TODO: Needs optimization
	 * @param sb
	 */
	private void doScatterAt(EntitySilbarn sb) {
		EntityPlayer spawner = getSpawner();
		
		int nRays = (int) ((dmg * 0.04) * GenericUtils.randIntv(30, 38));
		int scatRange = GenericUtils.randIntv(35, 50), hScat = scatRange / 2;
		for(int i = 0; i < nRays; ++i) {
			worldObj.spawnEntityInWorld(new EntityScatteredRay(this, sb, scatRange));
		}
		
		sb.onHitted();
		
		//Do the real damage.
//		AxisAlignedBB rawBound = AxisAlignedBB.getBoundingBox(sb.posX, sb.posY, sb.posZ, 
//				sb.posX, sb.posY, sb.posZ);
//		Motion3D mo0 = new Motion3D(getSpawner(), true).setPosition(sb.posX, sb.posY, sb.posZ);
		double judgeDist = 20;
		
		//Expand the raw bounds.
//		doExpand(rawBound, mo0, spawner.rotationYaw + scatRange, spawner.rotationPitch + hScat, judgeDist);
//		doExpand(rawBound, mo0, spawner.rotationYaw + scatRange, spawner.rotationPitch - hScat, judgeDist);
//		doExpand(rawBound, mo0, spawner.rotationYaw - scatRange, spawner.rotationPitch + hScat, judgeDist);
//		doExpand(rawBound, mo0, spawner.rotationYaw - scatRange, spawner.rotationPitch - hScat, judgeDist);
//		
//		System.out.println(rawBound);
		
		//Get all the entities within the raw bound and shrink the range.
		List<Entity> le = EntityUtils.getEntitiesAround(sb, judgeDist * 2, selector);
		for(Entity e : le) {
			//view orientation test
			double dx = e.posX - sb.posX, dy = e.posY - sb.posY, dz = e.posZ - sb.posZ;
			float yaw = -(float)(Math.atan2(dx, dz) * 180.0D / Math.PI);
			float pitch = -(float)(Math.atan2(dy, (double)Math.sqrt(dx * dx + dz * dz)) * 180.0D / Math.PI);
			
			if(Math.abs((yaw - spawner.rotationYawHead) % 360.0) <= scatRange &&
				Math.abs((pitch - spawner.rotationPitch) % 180.0) <= hScat) {
				if(rand.nextDouble() < 0.8)
				e.attackEntityFrom(DamageSource.causePlayerDamage(spawner), 
						(float) GenericUtils.randIntv(dmg * 0.5, dmg * 0.8));
			}
		}
		
	}
	
    private boolean canSeeEachOther(Entity e1, Entity e2) {
        return this.worldObj.rayTraceBlocks(this.worldObj.getWorldVec3Pool().getVecFromPool(
        	e1.posX, e1.posY + (double)e1.getEyeHeight(), e1.posZ), 
        	this.worldObj.getWorldVec3Pool().getVecFromPool(e2.posX, e2.posY + 
        	(double)e2.getEyeHeight(), e2.posZ)) == null;
    }
	
	private final IEntitySelector selector = new IEntitySelector() {

		@Override
		public boolean isEntityApplicable(Entity var1) {
			return !var1.equals(getSpawner()) && !(var1 instanceof EntitySilbarn) && !(var1 instanceof EntityScatteredRay);
		}
		
	};
	
	private void expandAABB(AxisAlignedBB aabb, double x, double y, double z) {
		aabb.minX = Math.min(x, aabb.minX);
		aabb.minY = Math.min(y, aabb.minY);
		aabb.minZ = Math.min(z, aabb.minZ);
		
		aabb.maxX = Math.max(x, aabb.maxX);
		aabb.maxY = Math.max(y, aabb.maxY);
		aabb.maxZ = Math.max(z, aabb.maxZ);
	}
	
	private void doExpand(AxisAlignedBB aabb, Motion3D ori, float yaw, float pitch, double dist) {
		Motion3D now =  ori.clone().calcMotionByRotation(yaw, pitch).move(dist);
		expandAABB(aabb, now.posX, now.posY, now.posZ);
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		seq.rebuild();
	}

	@Override
	public ResourceLocation[] getTexData() {
		return ACClientProps.ANIM_MD_RAY_L;
	}
	
	@SideOnly(Side.CLIENT)
	public static class RenderMD extends RendererRayTiling<EntityMeltDowner> {
		public RenderMD() {
			super(null);
			this.widthFp = 0.9;
			this.widthTp = 1.6;
		}
		
		protected ResourceLocation nextTexture(EntityMeltDowner ent, int i) {
			return ent.getTexData()[i == 0 ? 0 : ent.seq.get(i % ent.seq.size()) + 1];
		}
	}

}
