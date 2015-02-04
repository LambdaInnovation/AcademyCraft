/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
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
	
	public String getInternalName() {
		return "em_move";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_MOVE;
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
	
	@RegEntity(clientOnly = true)
	@RegEntity.HasRender
	@SideOnly(Side.CLIENT)
	public static class Ray extends EntityArcBase {
		
		final double x, y, z;
		
		@RegEntity.Render
		@SideOnly(Side.CLIENT)
		public static Render renderer;
		
		final HandleVel parent;

		public Ray(HandleVel ent) {
			super(ent.player, true);
			x = ent.tx;
			y = ent.ty;
			z = ent.tz;
			parent = ent;
			this.addEffectUpdate();
		}
		
		@Override
		public void onUpdate() {
			super.onUpdate();
			
			double dx = x - posX, dy = y - posY, dz = z - posZ;
			this.traceDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
			this.setHeading(dx, dy, dz, 1);
			
			if(parent.isDead)
				setDead();
		}
		
	}
	
	public static class Render extends RenderElecArc {
		
		public Render() {
			this.width = 0.2F;
		}
		
	}
	
	private static class MagState extends PatternHold.State {
		
		HandleVel handler;

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
				if(this.isRemote()) {
					player.worldObj.spawnEntityInWorld(new Ray(handler));
				}
			}
		}

		@Override
		public void onFinish() {
			if(handler != null)
				handler.setDead();
		}

		@Override
		public void onHold() {
			
		}
		
	}

}
