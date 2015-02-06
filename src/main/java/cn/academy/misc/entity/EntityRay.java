/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.entity.EntityWeakArc.OffSync;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * EntityRay is a entity class representing 'ray' like entities. You can specify the position, length, facing direction of
 * the ray. Also you can make the ray follow a creator EntityLivingBase(usually EntityPlayer)'s position and orientation.
 * For its associated renderer, see RenderRay.
 * @author WeathFolD
 */
public class EntityRay extends EntityX {
	
	Motion3D motion;
	
	EntityLivingBase thrower;
	protected boolean follow = false;
	
	protected boolean peformTrace = false;
	protected double traceDist;
	
	public boolean syncTrick = true; //if we will spawn at both client and server manually, for better effects
	public boolean isSync; //If this entity is being synced by MC mechanics(Currently derived, may not always be correct)
	
	@SideOnly(Side.CLIENT)
	public long unfoldTime = 0;
	
	public final long creationTime;
	
	public int fadeoutTime, tickFadeout;
	
	public boolean fading;
	
	public EntityRay(EntityLivingBase creator) {
		this(creator, true);
	}
	
	/**
	 * Use this ctor if the ray has a creator.
	 * @param world
	 * @param creator
	 * @param follow If the ray's position and heading will be calculate every tick according to the player.
	 */
	public EntityRay(EntityLivingBase creator, boolean follow) {
		this(creator.worldObj, new Motion3D(creator, true));
		thrower = creator;
		this.follow = follow;
	}
	
	/**
	 * Init an ray with fixed position and facing direction.
	 */
	public EntityRay(World world, Motion3D pos) {
		super(world);
		motion = pos;
		motion.applyToEntity(this);
		this.setCurMotion(new RayUpdate());
		setup();
		isSync = !worldObj.isRemote;
		creationTime = GenericUtils.getSystemTime();
	}

	/**
	 * Client-side ctor.
	 */
	@SideOnly(Side.CLIENT)
	public EntityRay(World world) {
		super(world);
		traceDist = getMaxDistance();
		//this.setCurMotion(new RayUpdate());
		setup();
		isSync = worldObj.isRemote;
		creationTime = GenericUtils.getSystemTime();
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(11, Integer.valueOf(0));
	}
	
	public void setFadeout(int tick) {
		fading = true;
		fadeoutTime = tick;
		tickFadeout = 0;
	}
	
	private void setup() {
		this.addDaemonHandler(new Sync());
		this.ignoreFrustumCheck = true;
	}
	
	/**
	 * This kind of entities are typically transparent, so we set the render pass in advance
	 */
	@Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
	
	public double getMaxDistance() {
		return 20.0;
	}
	
	public double getMinDistance() {
		return 0.0;
	}
	
	public double getTraceDistance() {
		return traceDist;
	}
	
	public void setPeformTrace(boolean is) {
		peformTrace = is;
	}
	
	public EntityLivingBase getThrower() {
		return thrower;
	}
	
	public MovingObjectPosition peformTrace() {
		return peformTrace(null);
	}
	
	public MovingObjectPosition peformTrace(IEntitySelector sel) {
		Motion3D tmp = motion.clone();
		Vec3 v1 = tmp.getPosVec(worldObj), v2 = tmp.move(getMaxDistance()).getPosVec(worldObj);
		return GenericUtils.rayTraceBlocksAndEntities(sel, worldObj, v1, v2, this, thrower);
	}
	
	private class Sync extends MotionHandler<EntityRay> {

		public Sync() {
			super(EntityRay.this);
		}

		@Override
		public void onCreated() {}

		@Override
		public void onUpdate() {
			if(!worldObj.isRemote) {
				dataWatcher.updateObject(11, thrower == null ? 0 : Integer.valueOf(thrower.getEntityId()));
			} else if(isSync) {
				int id = dataWatcher.getWatchableObjectInt(11);
				Entity e = worldObj.getEntityByID(id);
				if(e != null && e instanceof EntityLivingBase) {
					thrower = (EntityLivingBase) e;
				}
			}
		}

		@Override
		public String getID() {
			return "sync";
		}
		
	}
	
	private class RayUpdate extends MotionHandler<EntityRay> {

		public RayUpdate() {
			super(EntityRay.this);
		}

		@Override
		public void onCreated() {}

		@Override
		public void onUpdate() {
			if(motion == null) motion = new Motion3D(EntityRay.this, false);
			if(thrower != null) {
				if(follow) {
					motion.init(thrower, 0, true);
					motion.applyToEntity(EntityRay.this);
					rotationYaw = thrower.rotationYaw;
					rotationPitch = thrower.rotationPitch;
				}
			}
			
			if(peformTrace) {
				MovingObjectPosition mop = peformTrace();
				if(mop != null) {
					traceDist = Math.max(getMinDistance(), mop.hitVec.distanceTo(motion.getPosVec(worldObj)));
				} else {
					traceDist = getMaxDistance();
				}
			} else {
				traceDist = getMaxDistance();
			}
			
			if(fading) {
				if(++tickFadeout > fadeoutTime) {
					setDead();
				}
			}
		}

		@Override
		public String getID() {
			return "posmanip";
		}
		
	}

}
