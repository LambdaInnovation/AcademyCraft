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
package cn.academy.vanilla.meltdowner.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.RegInit;
import cn.liutils.entityx.EntityAdvanced;
import cn.liutils.entityx.EntityCallback;
import cn.liutils.entityx.event.CollideEvent;
import cn.liutils.entityx.event.CollideEvent.CollideHandler;
import cn.liutils.entityx.handlers.Rigidbody;
import cn.liutils.render.particle.Particle;
import cn.liutils.render.particle.ParticleFactory;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@Registrant
@RegInit(side = RegInit.Side.CLIENT_ONLY)
@RegEntity
@RegEntity.HasRender
public class EntitySilbarn extends EntityAdvanced {
	
	@SideOnly(Side.CLIENT)
	@RegEntity.Render
	public static RenderSibarn render;
	
	static ParticleFactory particles;
	@SideOnly(Side.CLIENT)
	public static void init() {
		Particle p = new Particle();
		p.texture = Resources.getTexture("entities/silbarn_frag");
		p.size = 0.1f;
		p.gravity = 0.03f;
		
		particles = new ParticleFactory(p);
	}
	
	boolean hit;
	
	long createTime;
	
	Vec3 axis = Vec3.createVectorHelper(rand.nextInt(), rand.nextInt(), rand.nextInt());
	
	{
		final Rigidbody rigidbody = new Rigidbody();
		rigidbody.linearDrag = 0.8;
		rigidbody.entitySel = EntitySelectors.nothing;
		
		this.addMotionHandler(rigidbody);
		//this.addDaemonHandler(new GravityApply(this, 0.05));
		executeAfter(new EntityCallback<EntitySilbarn>() {
			@Override
			public void execute(EntitySilbarn ent) {
				rigidbody.gravity = 0.12;
			}
		}, 50);
		setSize(.4f, .4f);
	}

	public EntitySilbarn(EntityPlayer player) {
		super(player.worldObj);
		this.regEventHandler(new CollideHandler() {
			
			@Override
			public void onEvent(CollideEvent event) {
				if(!hit) {
					hit = true;
					if(event.result.entityHit instanceof EntitySilbarn)
						playSound("academy:entity.silbarn_heavy", .5f, 1f);
					else
						playSound("academy:entity.silbarn_light", .5f, 1f);
					executeAfter(new EntityCallback() {
						@Override
						public void execute(Entity ent) {
							ent.setDead();
						}
					}, 10);
				}
			}
			
		});
		
		Motion3D mo = new Motion3D(player, true);
		mo.applyToEntity(this);
		
		this.rotationYaw = player.rotationYawHead;
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@SideOnly(Side.CLIENT)
	public EntitySilbarn(World world) {
		super(world);
		this.createTime = Minecraft.getSystemTime();
		
		this.regEventHandler(new CollideHandler() {
			@Override
			public void onEvent(CollideEvent event) {
				hit = true;
				MovingObjectPosition res = event.result;
				ForgeDirection dir = ForgeDirection.getOrientation(res.sideHit);
				final double mul = 0.1;
				double tx = res.hitVec.xCoord + dir.offsetX * mul, 
					ty = res.hitVec.yCoord + dir.offsetY * mul, 
					tz = res.hitVec.zCoord + dir.offsetZ * mul;
				spawnEffects(tx, ty, tz);
				setDead();
			}
		});
		
		this.isAirBorne = true;
		this.onGround = false;
	}
	
	@Override
	public void entityInit() {
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
	}
	
	public boolean isHit() {
		return hit;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		sync();
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
		int n = RandUtils.rangei(18, 27);
		for(int i = 0; i < n; ++i) {
			double vel = RandUtils.ranged(0.08, 0.18),
				vsq = vel * vel,
				vx = rand.nextDouble() * vel,
				vxsq = vx * vx,
				vy = rand.nextDouble() * Math.sqrt(vsq - vxsq),
				vz = Math.sqrt(vsq - vxsq - vy * vy);
			vx *= rand.nextBoolean() ? 1 : -1;
			vy *= rand.nextBoolean() ? 1 : -1;
			vz *= rand.nextBoolean() ? 1 : -1;
			vy += 0.2;
			
			particles.setPosition(posX, posY, posZ);
			particles.setVelocity(vx, vy, vz);
			worldObj.spawnEntityInWorld(particles.next(worldObj));
		}
		//TileMatrix
	}
	
	@SideOnly(Side.CLIENT)
	public static class RenderSibarn extends Render {
		
		private final IModelCustom model = Resources.getModel("silbarn");
		private final ResourceLocation tex = Resources.getTexture("models/silbarn");

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
			GL11.glRotated(0.03 * (Minecraft.getSystemTime() - sibarn.createTime), 
					sibarn.axis.xCoord, sibarn.axis.yCoord, sibarn.axis.zCoord);
			GL11.glRotated(-var1.rotationYaw, 0, 1, 0);
			GL11.glRotated(90, 1, 0, 0);
			model.renderAll();
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {}

}