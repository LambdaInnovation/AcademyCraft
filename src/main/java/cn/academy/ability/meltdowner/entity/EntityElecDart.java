package cn.academy.ability.meltdowner.entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.ability.meltdowner.client.render.RenderElecDart;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.MotionHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityElecDart extends EntityX {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderElecDart renderer;
	
	public EntityElecDart(World world) {
		super(world);
		if (!this.hasMotionHandler(HangAround.ID)) {
			this.setCurMotion(new HangAround(this));
		}
		this.handleClient = true;
	}

	public EntityElecDart(EntityPlayer player) {
		super(player.worldObj);
		this.posX = player.posX;
		this.posY = player.posY;
		this.posZ = player.posZ;
		if (!this.hasMotionHandler(HangAround.ID)) {
			this.setCurMotion(new HangAround(this));
		}
		this.centerY = this.posY;
		this.dataWatcher.updateObject(3, (float) this.centerY);
	}
	
	public void emit() {
	}

	@Override
	protected void entityInit() {
		super.entityInit();

		//this.dataWatcher.addObject(2, (int) syncTicksExisted);
		this.dataWatcher.addObject(3, (float) centerY);
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		if (!this.hasMotionHandler(HangAround.ID)) {
			this.setCurMotion(new HangAround(this));
		}
		//syncTicksExisted = var1.getInteger("ste");
		centerY = var1.getDouble("ldy");
		
		//dataWatcher.updateObject(2, (int) syncTicksExisted);
		dataWatcher.updateObject(3, (float) centerY);
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {
		//var1.setInteger("ste", syncTicksExisted);
		var1.setDouble("ldy", centerY);
	}
	
	//private int syncTicksExisted;
	private double centerY;
	

	public static class HangAround extends MotionHandler<EntityElecDart> {
		public static final String ID = "hangaround";

		public HangAround(EntityElecDart ent) {
			super(ent);
		}

		@Override
		public void onCreated() {}

		@Override
		public void onUpdate() {
			if (entity.worldObj.isRemote) {
				//entity.syncTicksExisted = entity.dataWatcher.getWatchableObjectInt(2);
				entity.centerY = entity.dataWatcher.getWatchableObjectFloat(3);
			}
			
			//++entity.syncTicksExisted;
			/*
			double wt = Math.sin(entity.syncTicksExisted / 20.0);
			entity.posY += (wt - entity.centerY) * 0.4;
			entity.centerY = wt;
			
			if (!entity.worldObj.isRemote) {
				entity.dataWatcher.updateObject(2, (int) entity.syncTicksExisted);
				entity.dataWatcher.updateObject(3, (float) entity.centerY);
			}*/
			double a = (entity.posY - entity.centerY);
			a = a / 0.1;
			if (a >= 1) {
				entity.posY = entity.centerY + 0.4 - 0.0003;
				entity.motionY = -0.001;//-0.001 * a;
				return;
			} else if (a <= -1) {
				entity.posY = entity.centerY - 0.4 + 0.0003;
				entity.motionY = 0.001;//0.001 * a;
				return;
			}
			a = Math.sqrt(1 - a * a);
			a = a * 0.01;
			if (a < 0.001) a = 0.001;
			if (entity.motionY > 0) entity.motionY = a;
			else entity.motionY = -a;
		}

		@Override
		public String getID() {
			return ID;
		}
		
	}
	
	@Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}
