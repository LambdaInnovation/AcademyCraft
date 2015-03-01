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
package cn.academy.ability.electro.skill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.entity.MoveArcRender;
import cn.academy.ability.electro.entity.EntityArcBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.misc.util.JudgeUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.FakeEntity;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The magnet movement ability. Makes the player fly towards some magnetic target
 * (Both blocks and entities).
 * @author WeathFolD
 */
@RegistrationClass
public class SkillMagMove extends SkillBase {
	
	static Map<EntityPlayer, Integer> entitiesToReduce = new HashMap();

	public SkillMagMove() {
		this.setLogo("electro/moving.png");
		setName("em_move");
		setMaxLevel(10);
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onPlayerFall(LivingFallEvent event) {
		if(entitiesToReduce.keySet().contains(event.entityLiving)) {
			event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		Iterator<Map.Entry<EntityPlayer, Integer>> iter = entitiesToReduce.entrySet().iterator();
		while(iter.hasNext()) {
			Map.Entry<EntityPlayer, Integer> entry = iter.next();
			if(entry.getValue() == 0) { iter.remove();  continue; }
			entry.setValue(entry.getValue() - 1);
		}
	}
	
	private static void playSound(EntityPlayer player, int n) {
		player.worldObj.playSoundAtEntity(player, "academy:elec.move." + n, 0.5f, 1.0f);
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternHold(1000) {

			@Override
			public State createSkill(EntityPlayer player) {
				return new MagState(player);
			}
			
		});
	}
	
	private static class HandleVel extends FakeEntity {
		
		final EntityPlayer player;
		double tx, ty, tz;
		double mox, moy, moz;
		
		boolean followEntity;
		Entity targ;
		
		final static double ACCEL = 0.08d;
		final double vel;

		public HandleVel(EntityPlayer target, double _tx, double _ty, double _tz, double _vel) {
			super(target);
			player = target;
			tx = _tx;
			ty = _ty;
			tz = _tz;
			vel = _vel;
			
			mox = target.motionX;
			moy = target.motionY;
			moz = target.motionZ;
		}
		
		public HandleVel(EntityPlayer target, Entity dest, double _vel) {
			this(target, dest.posX, dest.posY + dest.height * 0.7, dest.posZ, _vel);
			followEntity = true;
			targ = dest;
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			
			if(followEntity && !targ.isDead) {
				tx = targ.posX;
				ty = targ.posY + targ.height * 0.7;
				tz = targ.posZ;
			}
			
			double 
				dx = tx - player.posX,
				dy = ty - player.posY,
				dz = tz - player.posZ;
			
			if(Math.abs(GenericUtils.distanceSq(mox, moy, moz) - 
			GenericUtils.distanceSq(player.motionX, player.motionY, player.motionZ)) > 0.5) {
				mox = player.motionX;
				moy = player.motionY;
				moz = player.motionZ;
			}
			
			double mod = Math.sqrt(dx * dx + dy * dy + dz * dz) / vel;
			
			dx /= mod; dy /= mod; dz /= mod;
			
			mox = player.motionX = tryAdjust(mox, dx);
			moy = player.motionY = tryAdjust(moy, dy);
			moz = player.motionZ = tryAdjust(moz, dz);
		}
		
		private double tryAdjust(double from, double to) {
			double d = to - from;
			if(Math.abs(d) < ACCEL) {
				return to;
			}
			return d > 0 ? from + ACCEL : from - ACCEL;
		}
		
	}
	
	@RegEntity
	@RegEntity.HasRender
	public static class Ray extends EntityArcBase {
		
		float x, y, z;
		
		@RegEntity.Render
		@SideOnly(Side.CLIENT)
		public static MoveArcRender renderer;

		public Ray(HandleVel ent) {
			super(ent.player);
			x = (float) ent.tx;
			y = (float) ent.ty;
			z = (float) ent.tz;
		}
		
		@SideOnly(Side.CLIENT)
		public Ray(World world) {
			super(world);
		}
		
		@Override
		public void entityInit() {
			super.entityInit();
			dataWatcher.addObject(13, Float.valueOf(0));
			dataWatcher.addObject(14, Float.valueOf(0));
			dataWatcher.addObject(15, Float.valueOf(0));
		}
		
		@Override
		protected void syncServer() {
			super.syncServer();
			dataWatcher.updateObject(13, Float.valueOf(x));
			dataWatcher.updateObject(14, Float.valueOf(y));
			dataWatcher.updateObject(15, Float.valueOf(z));
		}
		
		@Override
		protected void syncClient() {
			super.syncClient();
			x = dataWatcher.getWatchableObjectFloat(13);
			y = dataWatcher.getWatchableObjectFloat(14);
			z = dataWatcher.getWatchableObjectFloat(15);
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			this.setByPoint(posX, posY, posZ, x, y, z);
		}
		
		@Override
		@SideOnly(Side.CLIENT)
		public void beforeRender() {
			this.setByPoint(posX, posY, posZ, x, y, z);
		}
		
		@Override
		public boolean doesFollowSpawner() {
			return true;
		}
		
	}
	
	private static class MagState extends PatternHold.State {
		
		HandleVel handler;
		Ray ray;
		final float csm;
		final double dist;
		
		final AbilityData data;
	
		private static ResourceLocation snd = new ResourceLocation("academy:elec.move");

		public MagState(EntityPlayer player) {
			super(player);
			data = AbilityDataMain.getData(player);
			int slv = data.getSkillLevel(CatElectro.magMovement), lv = data.getLevelID() + 1;
			dist = 18 + lv * 3 + slv * 1.2;
			csm = 20 - slv * 0.5f - lv;
		}

		@Override
		public void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			
			Motion3D mo = new Motion3D(player, true);
			Vec3 v1 = mo.getPosVec(player.worldObj), v2 = mo.move(dist).getPosVec(player.worldObj);
			MovingObjectPosition mop = GenericUtils.rayTraceBlocksAndEntities(null, player.worldObj, v1, v2, player);
			
			if(mop != null) {
				if((mop.typeOfHit == MovingObjectType.BLOCK && 
					JudgeUtils.isMetalBlock(player.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ)))
						|| (mop.typeOfHit == MovingObjectType.ENTITY && JudgeUtils.isEntityMetallic(mop.entityHit))) {
					player.worldObj.spawnEntityInWorld
					(handler = (
						mop.typeOfHit == MovingObjectType.BLOCK ? 
							new HandleVel(player, 
								mop.hitVec.xCoord, 
								mop.hitVec.yCoord + 0.8, 
								mop.hitVec.zCoord, 
								dist * 0.07) : 
							new HandleVel(player, mop.entityHit, dist * 0.07)
					));
					
					if(!isRemote()) {
						player.worldObj.playSoundAtEntity(player, "academy:elec.move", 0.5f, 1.0f);
						player.worldObj.spawnEntityInWorld(ray = new Ray(handler));
					}
					
					return; //fin starting action if successful
				}
			}
			//not successful
			if(isRemote()) {
				player.playSound("academy:deny", .5f, 1);
			}
			this.finishSkill(false);
		}
		
		@Override
		public boolean onTick(int ticks) {
			if(!isRemote() && (ticks - 1) % 40 == 0) {
				playSound(player, ((ticks - 1) / 40) % 5);
			}
			return !data.decreaseCP(csm);
		}

		@Override
		public boolean onFinish(boolean fin) {
			if(handler != null)
				handler.setDead();
			if(ray != null)
				ray.setDead();
			if(!player.worldObj.isRemote && data.getSkillLevel(CatElectro.magMovement) >= 5) {
				entitiesToReduce.put(player, 40); //2sec falling protection
			}
			return true;
		}

		@Override
		public void onHold() {
			
		}
		
	}

}
