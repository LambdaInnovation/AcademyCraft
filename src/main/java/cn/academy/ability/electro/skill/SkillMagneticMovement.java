/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.ability.electro.entity.EntityArcBase;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.FakeEntity;
import cn.liutils.util.DebugUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 吸引金属类方块而移动自身的能力，在原作中用来进行紧急回避，由于自伤太大所以较少使用……
 * TODO 施工中
 * TODO 关于自伤的考量？
 * @author WeathFolD
 */
@RegistrationClass
public class SkillMagneticMovement extends SkillBase {
	
	private static SkillMagneticMovement instance;

	public SkillMagneticMovement() {
		instance = this;
		this.setLogo("electro/moving.png");
		setName("em_move");
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
		final double tx, ty, tz;
		double mox, moy, moz;
		
		final static double ACCEL = 0.04d;
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
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			//player.motionY -= 0.08d; //cancel gravity
			
			double 
				dx = tx - player.posX,
				dy = ty - player.posY,
				dz = tz - player.posZ;
			
//			System.out.println(Math.abs(GenericUtils.distanceSq(mox, moy, moz) - 
//			GenericUtils.distanceSq(player.motionX, player.motionY, player.motionZ)));
			if(Math.abs(GenericUtils.distanceSq(mox, moy, moz) - 
			GenericUtils.distanceSq(player.motionX, player.motionY, player.motionZ)) > 0.5) {
				//System.out.println("Collision happened.");
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
		public static Render renderer;

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
		
		@SideOnly(Side.CLIENT)
		public void beforeRender() {
			this.setByPoint(posX, posY, posZ, x, y, z);
		}
		
		@Override
		public boolean doesFollowSpawner() {
			return true;
		}
		
	}
	
	public static class Render extends RenderElecArc {
		
		public Render() {
			this.width = 0.5F;
		}
		
	}
	
	private static class MagState extends PatternHold.State {
		
		HandleVel handler;
		Ray ray;

		public MagState(EntityPlayer player) {
			super(player);
		}

		@Override
		public void onStart() {
			AbilityData data = AbilityDataMain.getData(player);
			double dist = 30 + data.getLevelID() * 2.5 + data.getSkillLevel(data.getSkillID(instance)) * 0.6;
			
			MovingObjectPosition mop = GenericUtils.tracePlayer(player, dist);
			if(mop != null && mop.typeOfHit == MovingObjectType.BLOCK) {
				//player.worldObj.spawnEntityInWorld(new EntityBlockSimulator)
				player.worldObj.spawnEntityInWorld
				(handler = new HandleVel(player, mop.hitVec.xCoord, mop.hitVec.yCoord + 0.8, mop.hitVec.zCoord, 3));
				if(!isRemote()) {
					player.worldObj.spawnEntityInWorld(ray = new Ray(handler));
				}
			}
		}

		@Override
		public void onFinish() {
			if(handler != null)
				handler.setDead();
			if(ray != null)
				ray.setDead();
		}

		@Override
		public void onHold() {
			
		}
		
	}

}
