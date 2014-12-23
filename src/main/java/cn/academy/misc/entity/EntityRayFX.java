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
	}
	
	public EntityRayFX(World world, Motion3D motion, double len) {
		super(world);
		motion.applyToEntity(this);
		length = len;
	}
	
	@Override
	public void onUpdate() {
		//Do Nothing
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
