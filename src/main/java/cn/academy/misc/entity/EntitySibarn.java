/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.api.entityx.motion.GravityApply;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
public class EntitySibarn extends EntityX {
	
	boolean show = true;
	
	{
		final VelocityUpdate velUpdate;
		this.addDaemonHandler(velUpdate = new VelocityUpdate(this, .8));
		//this.addDaemonHandler(new GravityApply(this, 0.05));
		execAfter(50, new EntityCallback<EntitySibarn>() {
			@Override
			public void execute(EntitySibarn ent) {
				addDaemonHandler(new GravityApply(EntitySibarn.this, 0.09));
			}
		});
		setSize(.4f, .4f);
	}

	public EntitySibarn(EntityPlayer player) {
		super(player.worldObj);
		addDaemonHandler(new CollisionCheck(this) {
			@Override
			protected void onCollided(MovingObjectPosition res) {
				this.alive = false;
				show = false;
				execAfter(10, new EntityCallback() {
					@Override
					public void execute(EntityX ent) {
						ent.setDead();
					}
				});
			}
		}.addExclusion(player));
		Motion3D mo = new Motion3D(player, true);
		mo.applyToEntity(this);
		
//		motionX = GenericUtils.randIntv(-0.08, 0.08);
//		motionZ = GenericUtils.randIntv(-0.08, 0.08);
//		motionY = 0.3;
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	public EntitySibarn(World world) {
		super(world);
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@Override
	public void entityInit() {
		dataWatcher.addObject(10, Byte.valueOf((byte) 1));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		sync();
	}
	
	private void sync() {
		if(worldObj.isRemote) {
			boolean b = dataWatcher.getWatchableObjectByte(10) != 0;
			if(show && !b) {
				spawnEffects();
			}
			show = b;
		} else {
			dataWatcher.updateObject(10, Byte.valueOf((byte)(show ? 1 : 0)));
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void spawnEffects() {
	}

}
