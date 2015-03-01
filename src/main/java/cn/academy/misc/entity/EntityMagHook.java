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
package cn.academy.misc.entity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.register.ACItems;
import cn.academy.misc.client.render.RendererMagHook;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.api.entityx.motion.GravityApply;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityMagHook extends EntityX {
	
	{
		this.addDaemonHandler(new VelocityUpdate(this));
		this.addDaemonHandler(new GravityApply(this, 0.05));
		setSize(.5f, .5f);
	}
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static RendererMagHook renderer;
	
	public boolean isHit;
	public int hitSide;
	public int hookX, hookY, hookZ;
	
	boolean doesSetStill;
	
	public EntityMagHook(final EntityPlayer player) {
		super(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
		this.setHeading(motionX, motionY, motionZ, 2);
		this.addDaemonHandler(new CollisionCheck(this) {
			@Override
			protected void onCollided(MovingObjectPosition res) {
				if(res.typeOfHit == MovingObjectType.ENTITY) {
					if(!(res.entityHit instanceof EntityMagHook) || ((EntityMagHook)res.entityHit).isHit) {
						res.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(player), 4);
						dropAsItem();
					}
				} else {
					isHit = true;
					hitSide = res.sideHit;
					hookX = res.blockX;
					hookY = res.blockY;
					hookZ = res.blockZ;
					setStill();
				}
			}
		}.addExclusion(player));
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
		dataWatcher.addObject(11, Integer.valueOf(0));
		dataWatcher.addObject(12, Integer.valueOf(0));
		dataWatcher.addObject(13, Integer.valueOf(0));
	}
	
	@Override
	public void onUpdate() {
		if(this.doesSetStill) {
			doesSetStill = false;
			realSetStill();
		}
		super.onUpdate();
		sync();
	}
	
	@Override
	public void onCollideWithPlayer(EntityPlayer par1EntityPlayer) {
//		if(!worldObj.isRemote && ticksExisted > 20)
//			this.dropAsItem();
	}
	
	private void sync() {
		if(worldObj.isRemote) {
			boolean lastHit = isHit;
			byte b1 = dataWatcher.getWatchableObjectByte(10);
			isHit = (b1 & 1) != 0;
			hitSide = b1 >> 1;
			hookX = dataWatcher.getWatchableObjectInt(11);
			hookY = dataWatcher.getWatchableObjectInt(12);
			hookZ = dataWatcher.getWatchableObjectInt(13);
			if(!lastHit && isHit) {
				setStill();
			}
		} else {
			byte b1 = (byte) ((isHit ? 1 : 0) | (hitSide << 1));
			dataWatcher.updateObject(10, Byte.valueOf(b1));
			dataWatcher.updateObject(11, Integer.valueOf(hookX));
			dataWatcher.updateObject(12, Integer.valueOf(hookY));
			dataWatcher.updateObject(13, Integer.valueOf(hookZ));
		}
	}
	
    @Override
	public boolean interactFirst(EntityPlayer player) {
    	if(!worldObj.isRemote)
    		dropAsItem();
        return true;
    }
    
    @Override
    public boolean attackEntityFrom(DamageSource ds, float dmg) {
    	System.out.println(ds.getDamageType());
    	if(!worldObj.isRemote && ds.getEntity() instanceof EntityPlayer) {
    		dropAsItem();
    	}
    	return true;
    }
    
    @Override
	public boolean canBeCollidedWith() {
        return this.isHit;
    }
    
    private void dropAsItem() {
    	worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, new ItemStack(ACItems.magHook)));
    	setDead();
    }
    
    private void setStill() {
    	this.doesSetStill = true;
    }
    
    private void realSetStill() {	
    	motionX = motionY = motionZ = 0;
    	if(worldObj != null) {
    		worldObj.playSoundAtEntity(this, "academy:maghook_land", .8f, 1.0f);
    	}
    	this.setSize(1f, 1f);
    	this.clearDaemonHandlers();
    	this.addDaemonHandler(new MotionHandler(this) {

			@Override
			public void onUpdate() {
				preRender();
				if(!worldObj.isRemote) {
					//Check block consistency
					if(worldObj.isAirBlock(hookX, hookY, hookZ)) {
						dropAsItem();
					}
				}
			}

			@Override
			public String getID() {
				return "huh";
			}
    		
    	});
    }

	@SideOnly(Side.CLIENT)
	public EntityMagHook(World world) {
		super(world);
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean("isHit", isHit);
		tag.setInteger("hitSide", hitSide);
		tag.setInteger("hookX", hookX);
		tag.setInteger("hookZ", hookZ);
		tag.setInteger("hookZ", hookZ);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		isHit = tag.getBoolean("isHit");
		hitSide = tag.getInteger("hitSide");
		hookX = tag.getInteger("hookX");
		hookY = tag.getInteger("hookY");
		hookZ = tag.getInteger("hookZ");
		if(isHit) {
			setStill();
		}
	}
	
	public void preRender() {
		if(this.isHit) {
			switch(hitSide) {
			case 0:
				rotationPitch = -90; break;
			case 1:
				rotationPitch = 90; break;
			case 2:
				rotationYaw = 0; rotationPitch = 0; break;
			case 3:
				rotationYaw = 180; rotationPitch = 0; break;
			case 4:
				rotationYaw = -90; rotationPitch = 0; break;
			case 5:
				rotationYaw = 90; rotationPitch = 0; break;
			}
			ForgeDirection fd = ForgeDirection.getOrientation(hitSide);
			setPosition(hookX + 0.5 + fd.offsetX * 0.51, 
					hookY + 0.5 + fd.offsetY * 0.51, 
					hookZ + 0.5 + fd.offsetZ * 0.51);
		}
	}

}
