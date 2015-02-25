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

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.academy.misc.entity.fx.EntitySibarnFrag;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.CollisionCheck;
import cn.liutils.api.entityx.motion.GravityApply;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;
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
public class EntitySilbarn extends EntityX {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderSibarn render;
	
	boolean hit;
	
	@SideOnly(Side.CLIENT)
	long createTime = GenericUtils.getSystemTime();
	Vec3 axis = Vec3.createVectorHelper(rand.nextInt(), rand.nextInt(), rand.nextInt());
	
	{
		final VelocityUpdate velUpdate;
		this.addDaemonHandler(velUpdate = new VelocityUpdate(this, .8));
		//this.addDaemonHandler(new GravityApply(this, 0.05));
		execAfter(50, new EntityCallback<EntitySilbarn>() {
			@Override
			public void execute(EntitySilbarn ent) {
				addDaemonHandler(new GravityApply(EntitySilbarn.this, 0.12));
			}
		});
		setSize(.4f, .4f);
	}

	public EntitySilbarn(EntityPlayer player) {
		super(player.worldObj);
		addDaemonHandler(new CollisionCheck(this) {
			@Override
			protected void onCollided(MovingObjectPosition res) {
				this.alive = false;
				execAfter(10, new EntityCallback() {
					@Override
					public void execute(EntityX ent) {
						ent.setDead();
					}
				});
			}
		}.setBlockOnly());
		Motion3D mo = new Motion3D(player, true);
		mo.applyToEntity(this);
		
		this.rotationYaw = player.rotationYawHead;
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	
	public EntitySilbarn(World world) {
		super(world);
		addDaemonHandler(new CollisionCheck(this) {
			@Override
			protected void onCollided(MovingObjectPosition res) {
				ForgeDirection dir = ForgeDirection.getOrientation(res.sideHit);
				final double mul = 0.1;
				double tx = res.hitVec.xCoord + dir.offsetX * mul, 
					ty = res.hitVec.yCoord + dir.offsetY * mul, 
					tz = res.hitVec.zCoord + dir.offsetZ * mul;
				spawnEffects(tx, ty, tz);
				setDead();
			}
		}.setBlockOnly());
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@Override
	public void entityInit() {
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		sync();
	}
	
	public void onHitted() {
		hit = true;
		this.playSound("academy:silbarn_heavy", .5f, 1f);
		this.execAfter(10, new EntityCallback() {
			@Override
			public void execute(EntityX ent) {
				ent.setDead();
			}
		});
	}
	
	private void sync() {
		if(worldObj.isRemote) {
			boolean b = dataWatcher.getWatchableObjectByte(10) != 0;
			if(!hit && b) {
				spawnEffects(posX, posY, posZ);
			}
			hit = b;
		} else {
			dataWatcher.updateObject(10, Byte.valueOf((byte) (hit ? 1 : 0)));
		}
	}
	
    @Override
	public boolean canBeCollidedWith() {
        return true;
    }
	
	@SideOnly(Side.CLIENT)
	private void spawnEffects(double tx, double ty, double tz) {
		int n = GenericUtils.randIntv(18, 27);
		for(int i = 0; i < n; ++i) {
			double vel = GenericUtils.randIntv(0.08, 0.28),
				vsq = vel * vel,
				vx = rand.nextDouble() * vel,
				vxsq = vx * vx,
				vy = rand.nextDouble() * Math.sqrt(vsq - vxsq),
				vz = Math.sqrt(vsq - vxsq - vy * vy);
			vx *= rand.nextBoolean() ? 1 : -1;
			vy *= rand.nextBoolean() ? 1 : -1;
			vz *= rand.nextBoolean() ? 1 : -1;
			vy += 0.2;
			worldObj.spawnEntityInWorld(new EntitySibarnFrag(worldObj, tx, ty, tz, vx, vy, vz));
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static class RenderSibarn extends Render {
		
		private final IModelCustom model = ACModels.MDL_SILBARN;
		private final ResourceLocation tex = ACClientProps.TEX_MDL_SILBARN;

		@Override
		public void doRender(Entity var1, double x, double y,
				double z, float var8, float var9) {
			EntitySilbarn sibarn = (EntitySilbarn) var1;
			if(sibarn.hit)
				return;
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			RenderUtils.loadTexture(tex);
			double scale = .05;
			GL11.glScaled(scale, scale, scale);
			GL11.glRotated(0.03 * (GenericUtils.getSystemTime() - sibarn.createTime), 
					sibarn.axis.xCoord, sibarn.axis.yCoord, sibarn.axis.zCoord);
			GL11.glRotated(-var1.rotationYaw, 0, 1, 0);
			GL11.glRotated(90, 1, 0, 0);
			model.renderAll();
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
