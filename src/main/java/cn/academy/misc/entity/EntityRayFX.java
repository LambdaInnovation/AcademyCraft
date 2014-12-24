/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cn.liutils.api.util.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 一般的光线效果
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class EntityRayFX extends Entity {
	
	double MAX_LENGTH = 50.0;
	double length;
	protected boolean followPlayer = false; //是否跟随玩家运动
	protected final EntityPlayer player;
	protected int lifeTime = -1;
	private Motion3D motion;
	
	public EntityRayFX(World world, EntityPlayer player) {
		this(world, new Motion3D(player, true));
	}
	
	public EntityRayFX(World world, Motion3D motion) {
		super(world);
		motion.applyToEntity(this);
		MovingObjectPosition mop = motion.applyRaytrace(world);
		if(mop == null) {
			length = MAX_LENGTH;
		} else {
			length = motion.getPosVec(world).distanceTo(mop.hitVec);
		}
		this.motion = new Motion3D(motion);
		this.player = null;
	}
	
	public EntityRayFX(World world, Motion3D motion, double len) {
		super(world);
		motion.applyToEntity(this);
		this.motion = new Motion3D(motion);
		length = len;
		this.player = null;
	}
	
	public EntityRayFX setFollowPlayer(boolean b) {
		followPlayer = b;
		return this;
	}
	
	public EntityRayFX setLifetime(int i) {
		lifeTime = i;
		return this;
	}
	
	@Override
	public void onUpdate() {
		if(followPlayer && player != null) {
			motion.init(player, 0, true);
			motion.applyToEntity(this);
		}
		if(lifeTime > 0 && ticksExisted > lifeTime) {
			setDead();
		}
	}
	
	public double getLength() {
		return length;
	}

	@Override
	protected void entityInit() {}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		this.setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {}

}
