/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.FollowEntity;
import cn.liutils.api.entityx.motion.VelocityUpdate;
import cn.liutils.template.client.render.entity.RenderIcon;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Basic ball class. Implemented a very simple ticked event system.
 * It will automatically follow the player and float around.</br>
 * For better render effects, this class used hacking on positions. Use getPosition() to get real coordinates.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(freq = 2)
@RegEntity.HasRender
public class EntityMdBall extends EntityX {
	
	static ResourceLocation[] 
		TEX_NORMAL, 
		TEX_ACTIVE;
	static {
		TEX_NORMAL = new ResourceLocation[5];
		for(int i = 0; i < 5; ++i) {
			TEX_NORMAL[i] = new ResourceLocation("academy:textures/effects/mdball/" + i + ".png");
		}
		
		TEX_ACTIVE = new ResourceLocation[4];
		for(int i = 0; i < 4; ++i) {
			TEX_ACTIVE[i] = new ResourceLocation("academy:textures/effects/mdball_active/" + i + ".png");
		}
	}
	
	boolean load = false;
	EntityPlayer spawner;
	double offx, offy, offz;
	
	final int timeOffset = rand.nextInt(233333);
	
	protected int fadeTime = 10;
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static BallRender render;
	
	public enum BallState { 
		NORMAL(TEX_NORMAL), ACTIVE(TEX_ACTIVE);
		public ResourceLocation[] texs;
		BallState(ResourceLocation[] _t) {
			texs = _t;
		}
	};
	
	Map<Integer, List<Callback>> events = new HashMap();
	private BallState state = BallState.NORMAL;
	
	int texID = 0;
	
	public EntityMdBall(EntityPlayer player) {
		super(player.worldObj);
		spawner = player;
		offx = (rand.nextBoolean() ? 1 : -1) * GenericUtils.randIntv(0.5, 0.8);
		offy = GenericUtils.randIntv(1.2, 2);
		offz = (rand.nextBoolean() ? 1 : -1) * GenericUtils.randIntv(0.5, 0.8);
		addDaemonHandler(new FollowEntity(this, player).setOffset(
				offx, offy, offz));
		setPosition(player.posX + offx, player.posY + offy, player.posZ + offz);
		addDaemonHandler(new VelocityUpdate(this, 1));
	}

	public EntityMdBall(World world) {
		super(world);
		addDaemonHandler(new VelocityUpdate(this, 1));
		ignoreFrustumCheck = true;
	}
	
	@Override
	public void entityInit() {
		super.entityInit();
		dataWatcher.addObject(10, Byte.valueOf((byte) 0));
		dataWatcher.addObject(11, Integer.valueOf(0));
		dataWatcher.addObject(12, Float.valueOf(0));
		dataWatcher.addObject(13, Float.valueOf(0));
		dataWatcher.addObject(14, Float.valueOf(0));
	}
	
	public void execAfter(int ticks, Callback bc) {
		List<Callback> res = events.get(ticks);
		if(res == null) {
			res = new ArrayList();
			events.put(ticks, res);
		}
		res.add(bc);
	}
	
	@Override
	public void onUpdate() {
		if(spawner != null && doesFollow()) {
			motionX = spawner.motionX;
			motionY = spawner.motionY;
			motionZ = spawner.motionZ;
		}
		
		super.onUpdate();
		//System.out.println(posX + " " + worldObj.isRemote);
		//System.out.println(offy + " " + worldObj.isRemote);
		List<Callback> res = events.get(ticksExisted);
		if(res != null) {
			for(Callback cb : res)
				cb.action(this);
		}
		
		texID = rand.nextInt(state.texs.length);
		sync();
	}
	
	private void sync() {
		if(!worldObj.isRemote) {
			dataWatcher.updateObject(10, Byte.valueOf((byte) state.ordinal()));
			dataWatcher.updateObject(11, Integer.valueOf(spawner.getEntityId()));
			
			dataWatcher.updateObject(12, Float.valueOf((float) offx));
			dataWatcher.updateObject(13, Float.valueOf((float) offy));
			dataWatcher.updateObject(14, Float.valueOf((float) offz));
		} else {
			state = BallState.values()[dataWatcher.getWatchableObjectByte(10)];
			Entity ent = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(11));
			offx = dataWatcher.getWatchableObjectFloat(12);
			offy = dataWatcher.getWatchableObjectFloat(13);
			offz = dataWatcher.getWatchableObjectFloat(14);
			if(ent instanceof EntityPlayer && !load) {
				load = true;
				spawner = (EntityPlayer) ent;
//				addDaemonHandler(new FollowEntity(this, ent)
//						.setOffset(offx, offy - 1.6, offz));
				//System.out.println("real load in client");
			}
		}
	}
	
	/**
	 * Get real position for this md ball.
	 */
	public double[] getPosition() {
		if(spawner == null) {
			return new double[] { posX, posY, posZ };
		}
		return new double[] {
			spawner.posX + offx,
			spawner.posY + (worldObj.isRemote ? 0 : 1.6) + offy,
			spawner.posZ + offz
		};
	}
	
	public ResourceLocation getTexture() {
		return state.texs[texID % state.texs.length];
	}
	
	public void setState(BallState ns) {
		if(ns != state)
			texID = 0;
		state = ns;
	}
	
    @Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound) {
    	setDead();
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

	protected boolean doesFloat() { return true; }
	
	protected boolean doesFollow() {
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	protected double getAlpha() {
		return Math.min(1.0, (double)ticksExisted / fadeTime);
	}
	
	public static interface Callback {
		void action(EntityMdBall ball);
	}
	
	@SideOnly(Side.CLIENT)
	public static class BallRender extends RenderIcon {

		public BallRender() {
			super(null);
			setSize(0.5f);
			setHasLight(false);
			setBlend(0.8f);
		}
		
		@Override
		public void doRender(Entity ent, double x, double y,
				double z, float f, float g) {
			EntityPlayer clientPlayer = Minecraft.getMinecraft().thePlayer;
			EntityMdBall ball = (EntityMdBall) ent;
			if(ball.spawner == null)
				return;
			
			boolean firstPerson = 
					Minecraft.getMinecraft().gameSettings.thirdPersonView == 0 
					&& clientPlayer.equals(ball.spawner);
			long time = Minecraft.getSystemTime();
			
			//position hack
			if(ball.doesFollow()) {
				x = ball.offx + (ball.spawner.posX - clientPlayer.posX);
				y = ball.offy - 1.6  + (ball.spawner.posY - clientPlayer.posY);
				z = ball.offz  + (ball.spawner.posZ - clientPlayer.posZ);
			}
			if(ball.doesFloat()) 
				y += 0.1 * Math.sin((time - ball.timeOffset) / 700D);
			
			icon = ball.getTexture();
			this.alpha = 0.8 * ball.getAlpha();
			
			super.doRender(ent, x, y, z, f, g);
		}
		
	}

}
