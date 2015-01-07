/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.misc.client.render.RendererCoin;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.api.entityx.motion.GravityApply;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renders the throwing coin after it had been thrown.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(renderName = "renderer")
public class EntityThrowingCoin extends EntityX {
	
	public static class AvoidSync extends EntityThrowingCoin { //user client-side generate
		public AvoidSync(EntityPlayer player, ItemStack is) {
			super(player, is);
		}
	}
	
	private class KeepPosition extends MotionHandler<EntityThrowingCoin> {

		public KeepPosition() {
			super(EntityThrowingCoin.this);
		}
		
		@Override
		public void onSpawnedInWorld() {}

		@Override
		public void onUpdate() {
			if(EntityThrowingCoin.this.player != null) {
				posX = player.posX;
				posZ = player.posZ;
				if(posY < player.posY || ticksExisted > MAXLIFE) {
					setDead();
				}
			}
			
			maxHt = Math.max(maxHt, posY);
			if(worldObj.isRemote) {
				initHt = dataWatcher.getWatchableObjectFloat(10);
			} else {
				dataWatcher.updateObject(10, Float.valueOf(initHt));
			}
		}

		@Override
		public String getID() {
			return "kip";
		}
		
	}
	
	@SideOnly(Side.CLIENT)
	public static RendererCoin renderer;
	
	private static final int MAXLIFE = 120;
	private static final double INITVEL = 0.65;
	
	private float initHt;
	private double maxHt;
	public EntityPlayer player;
	public ItemStack stack;
	public Vec3 axis;
	public boolean isSync = false;
	
	public EntityThrowingCoin(World world) {
		super(world);
		isSync = true;
		setup();
	}
	
	public EntityThrowingCoin(EntityPlayer player, ItemStack is) {
		super(player.worldObj);
		this.stack = is;
		this.player = player;
		this.initHt = (float) player.posY;
		setPosition(player.posX, player.posY, player.posZ);
		setup();
		dataWatcher.updateObject(10, Float.valueOf(initHt));
	}
	
	private void setup() {
		this.removeDaemonHandler("collision");
		this.addDaemonHandler(new GravityApply(this, 0.05));
		this.setCurMotion(new KeepPosition());
		this.handleClient = true;
		this.motionY = INITVEL;
		axis = Vec3.createVectorHelper(.1 + rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
		this.getEntityData().setLong("startTime", GenericUtils.getSystemTime());
	}
	
	public double getProgress() {
		if(motionY > 0) { //Throwing up
			return (INITVEL - motionY) / INITVEL * 0.5;
		} else {
			return Math.min(1.0, 0.5 + ((maxHt - posY) / (maxHt - initHt)) * 0.5);
		}
	}
	
	@Override
	public void entityInit() {
		this.dataWatcher.addObject(10, Float.valueOf(0));
	}
}
