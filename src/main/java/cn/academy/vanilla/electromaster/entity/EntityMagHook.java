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
package cn.academy.vanilla.electromaster.entity;

import cn.academy.vanilla.ModuleVanilla;
import cn.academy.vanilla.electromaster.client.renderer.RendererMagHook;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.entityx.MotionHandler;
import cn.liutils.entityx.event.CollideEvent;
import cn.liutils.entityx.event.CollideEvent.CollideHandler;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author WeathFolD
 *
 */
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityMagHook extends EntityAdvanced {
	
	{
		Rigidbody rb = new Rigidbody();
		rb.gravity = 0.05;
		addMotionHandler(rb);
		setSize(.5f, .5f);
	}
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RendererMagHook renderer;
	
	public boolean isHit;
	public int hitSide;
	public int hookX, hookY, hookZ;
	
	boolean doesSetStill;
	
	public EntityMagHook(final EntityPlayer player) {
		super(player.worldObj);
		new Motion3D(player, true).multiplyMotionBy(2).applyToEntity(this);
		
		Rigidbody rb = this.getMotionHandler(Rigidbody.class);
		rb.entitySel = EntitySelectors.excludeOf(player);
		
		this.regEventHandler(new CollideHandler() {

			@Override
			public void onEvent(CollideEvent event) {
				MovingObjectPosition res = event.result;
				if(res.typeOfHit == MovingObjectType.ENTITY) {
					if(!(res.entityHit instanceof EntityMagHook) || ((EntityMagHook)res.entityHit).isHit) {
						if(!(res.entityHit instanceof EntityMagHook))
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
			
		});
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	public EntityMagHook(World world) {
		super(world);
		this.isAirBorne = true;
		this.onGround = false;
		this.ignoreFrustumCheck = true;
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
		//System.out.println("sync " + posX + " " + posY + " " + posZ + " " + worldObj.isRemote + " " + isHit + " " + this);
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
    public boolean attackEntityFrom(DamageSource ds, float dmg) {
    	if(isHit && !worldObj.isRemote && ds.getEntity() instanceof EntityPlayer) {
    		dropAsItem();
    	}
    	return true;
    }
    
    @Override
	public boolean canBeCollidedWith() {
        return this.isHit;
    }
    
    private void dropAsItem() {
    	worldObj.spawnEntityInWorld(new EntityItem(worldObj, posX, posY, posZ, new ItemStack(ModuleVanilla.magHook)));
    	setDead();
    }
    
    private void setStill() {
    	this.doesSetStill = true;
    }
    
    private void realSetStill() {	
    	motionX = motionY = motionZ = 0;
    	if(worldObj != null) {
    		//worldObj.playSoundAtEntity(this, "academy:maghook_land", .8f, 1.0f);
    	}
    	this.setSize(1f, 1f);
    	this.removeMotionHandlers();
    	this.addMotionHandler(new MotionHandler() {

    		@Override
			public void onStart() {}
    		
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
	
	@Override
	public void writeEntityToNBT(NBTTagCompound tag) {
		tag.setBoolean("isHit", isHit);
		tag.setInteger("hitSide", hitSide);
		tag.setInteger("hookX", hookX);
		tag.setInteger("hookY", hookY);
		tag.setInteger("hookZ", hookZ);
	}
	
	@Override
	public void readEntityFromNBT(NBTTagCompound tag) {
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
