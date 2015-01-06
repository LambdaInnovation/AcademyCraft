/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.liutils.util.DebugUtils;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cn.weaponmod.api.damage.Damage;
import cn.weaponmod.api.damage.DmgSimple;

/**
 * @author WeathFolD
 */
public abstract class EntityRay extends Entity {
	
	int HIT_WAIT = 30;
	protected Damage damage;
	public double curX, curY, curZ;
	public boolean hit = false;
	public int tickAfterHit = 0;

	EntityLivingBase thrower; // thrower, if any
	
	public EntityRay(World world, Motion3D pos, Damage dmg) {
		super(world);
		pos.applyToEntity(this);
		damage = dmg;
		curX = posX;
		curY = posY;
		curZ = posZ;
		setVelocity(3.0F);
	}

	public EntityRay(EntityLivingBase elb, Damage dmg) {
		this(elb.worldObj, new Motion3D(elb, true), dmg);
		thrower = elb;
	}
	
	public EntityRay(World world, Motion3D pos, EntityLivingBase elb, float dmg) {
		this(world, pos, new DmgSimple(DamageSource.causeMobDamage(elb), dmg));
		thrower = elb;
	}
	
	public EntityRay(EntityLivingBase elb, float dmg) {
		this(elb, new DmgSimple(DamageSource.causeMobDamage(elb), dmg));
	}
	
	public EntityRay(World world) {
		super(world);
	}
	
	@Override
	public void entityInit() {
		this.ignoreFrustumCheck = true;
		dataWatcher.addObject(10, Float.valueOf(0));
		dataWatcher.addObject(11, Float.valueOf(0));
		dataWatcher.addObject(12, Float.valueOf(0));
	}
	
	public void setVelocity(float f) {
		motionX *= f;
		motionY *= f;
		motionZ *= f;
	}
	
	@Override
	public void onUpdate() {
		//super.onUpdate();
		//Update motion
		
		if(worldObj.isRemote) {
			curX = dataWatcher.getWatchableObjectFloat(10);
			curY = dataWatcher.getWatchableObjectFloat(11);
			curZ = dataWatcher.getWatchableObjectFloat(12);
		} else {
			if(curX == 0 && curY == 0 && curZ == 0) {
				curX = posX;
				curY = posY;
				curZ = posZ;
			}
			Vec3 now = worldObj.getWorldVec3Pool().getVecFromPool(curX, curY, curZ);
			curX += motionX;
			curY += motionY;
			curZ += motionZ;
			Vec3 after = worldObj.getWorldVec3Pool().getVecFromPool(curX, curY, curZ);
			MovingObjectPosition mop = GenericUtils.rayTraceBlocksAndEntities(null, worldObj, now, after, thrower);
			if(mop != null) {
				onImpact(mop);
			}
			dataWatcher.updateObject(10, Float.valueOf((float) curX));
			dataWatcher.updateObject(11, Float.valueOf((float) curY));
			dataWatcher.updateObject(12, Float.valueOf((float) curZ));
		}
		
		if(hit) {
			++tickAfterHit;
			if(tickAfterHit > HIT_WAIT) {
				this.setDead();
			}
		}
		if(ticksExisted > 30) setDead();
	}

	protected void onImpact(MovingObjectPosition res) {
		if(worldObj.isRemote || hit) return;
		if(res.typeOfHit == MovingObjectType.ENTITY) {
			onEntityHit(res.entityHit);
		} else {
			onBlockHit(res.blockX, res.blockY, res.blockZ);
		}
		hit = true;
	}
	
	protected void onEntityHit(Entity e) {
		if(damage != null)
			damage.damageEntity(e);
	}
	
	protected void onBlockHit(int x, int y, int z) {
		
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {}
}
