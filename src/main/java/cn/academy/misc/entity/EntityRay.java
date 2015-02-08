package cn.academy.misc.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class EntityRay extends EntityX {

	protected float rayLength = getDefaultRayLen();
	
	Motion3D motion;
	
	EntityLivingBase spawner;
	
	boolean load;
	
	@SideOnly(Side.CLIENT)
	protected double alpha = 1.0;
	
	{
		ignoreFrustumCheck = true;
	}
	
	public EntityRay(EntityLivingBase _spawner) {
		super(_spawner.worldObj);
		spawner = _spawner;
		motion = new Motion3D(spawner, true);
		motion.applyToEntity(this);
		
		if(spawner != null) {
			if(doesFollowSpawner()) {
				addDaemonHandler(new FollowSpawner());
			}
			if(doesPerformTrace()) {
				addDaemonHandler(new DoTrace());
			}
		}
		
		load = true;
	}
	
	@SideOnly(Side.CLIENT)
	public EntityRay(World world) {
		super(world);
		load = false;
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
		dataWatcher.addObject(11, Float.valueOf(getDefaultRayLen()));
		dataWatcher.addObject(12, Integer.valueOf(-1));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(worldObj.isRemote) { 
			syncClient();
		} else { 
			syncServer();
		};
	}
	
	@SideOnly(Side.CLIENT)
	protected void syncClient() {
		boolean lastLoad = load;
		
		load = dataWatcher.getWatchableObjectByte(10) != 0;
		rayLength = dataWatcher.getWatchableObjectFloat(11);
		int eid = dataWatcher.getWatchableObjectInt(12);
		Entity elb = worldObj.getEntityByID(eid);
		if(elb instanceof EntityLivingBase) {
			spawner = (EntityLivingBase) elb;
		}
		
		if(!lastLoad && load) {
			onClientInit();
		}
	}
	
	protected void syncServer() {
		dataWatcher.updateObject(10, Byte.valueOf((byte) (load ? 1 : 0)));
		dataWatcher.updateObject(11, Float.valueOf(rayLength));
		dataWatcher.updateObject(12, spawner == null ? -1 : spawner.getEntityId());
	}
	
	/**
	* Called when the client entity first receives sync data.
	*/
	@SideOnly(Side.CLIENT)
	protected void onClientInit() {
		if(spawner != null) {
			if(doesFollowSpawner()) {
				addDaemonHandler(new FollowSpawner());
			}
			if(doesPerformTrace()) {
				addDaemonHandler(new DoTrace());
			}
		}
	}
	
	protected float getDefaultRayLen() {
		return 20.0f;
	}
	
	protected boolean doesPerformTrace() {
		return true;
	}
	
	public boolean doesFollowSpawner() {
		return false;
	}
	
	public EntityLivingBase getSpawner() {
		return spawner;
	}
	
	public boolean isLoaded() {
		return load;
	}
	
	public float getRayLength() { //Get the REAL length of the entity.
		return rayLength;
	}
	
	public float getDisplayRayLen() { //This enables comlicated display tricks on ray len.
		return rayLength;
	}
	
	@Override
	public float getRotationYawHead() {
		return rotationYaw;
	}
	
	@SideOnly(Side.CLIENT)
	public double getAlpha() {
		return alpha;
	}
	
	public void setFadeout(final int tick) {
		this.addDaemonHandler(new MotionHandler(this) {
			int count = 0;
			
			@Override
			public void onUpdate() {
				alpha = (double)(tick - count) / tick;
				if(count >= tick) {
					setDead();
				}
				++count;
			}

			@Override
			public String getID() {
				return "fadeout";
			}
			
		});
	}
	
	/*
	* Cartesian coordinate support
	*/
	public void setByPoint(double x0, double y0, double z0, double x1, double y1, double z1) {
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		double tmp = dx * dx + dz * dz;
		setPosition(x0, y0, z0);
		rayLength = MathHelper.sqrt_double(tmp + dy * dy);
		rotationYaw = (float) -(Math.atan2(dx, dz) * 180 / Math.PI);
		rotationPitch = (float) -(Math.atan2(dy, Math.sqrt(tmp)) * 180 / Math.PI);
	}
	
	public void setBySpawner() {
		if(spawner != null) {
			if(motion == null) motion = new Motion3D();
			motion.init(spawner, 0, true);
			motion.applyToEntity(this);
		}
	}
	
	public MovingObjectPosition performTrace() {
		Motion3D mo = new Motion3D(this, true);
		Vec3 v1 = mo.getPosVec(worldObj), v2 = mo.move(getDefaultRayLen()).getPosVec(worldObj);
		return GenericUtils.rayTraceBlocksAndEntities(null, worldObj, v1, v2, this, getSpawner());
	}
	
	@SideOnly(Side.CLIENT)
	public void beforeRender() { //Called by render to update position&rotation, for better viewing effects.
		if(doesFollowSpawner()) {
			setPosition(spawner.posX, spawner.posY, spawner.posZ);
			rotationPitch = spawner.rotationPitch;
			rotationYaw = spawner.rotationYaw;
		}
	}
	
	@Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
	
	private class FollowSpawner extends MotionHandler<EntityRay> {
		public FollowSpawner() {
			super(EntityRay.this);
		}
		
		@Override 
		public void onUpdate() {
			setBySpawner();
		}
			
		@Override 
		public String getID() { 
			return "follow";
		}
	}
	
	private class DoTrace extends MotionHandler<EntityRay> {

		public DoTrace() {
			super(EntityRay.this);
		}
		
		@Override 
		public void onUpdate() {
				MovingObjectPosition res = performTrace();
				if(res == null) {
					rayLength = getDefaultRayLen();
					return;
				}
				rayLength = MathHelper.sqrt_double(res.hitVec.squareDistanceTo(posX, posY, posZ));
		}
			
		@Override 
		public String getID() { 
			return "trace";
		}
	}

}