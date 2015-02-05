/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import cn.academy.ability.electro.entity.fx.EntityArcS;
import cn.academy.ability.electro.skill.SkillRailgun;
import cn.academy.api.data.AbilityData;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.annoreg.mc.RegEntity.HasRender;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@HasRender
public class EntityRailgun extends EntityRay {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static Render renderer;
	
	@SideOnly(Side.CLIENT)
	public static class Render extends RendererRayTiling<EntityRailgun> {
		
		public Render() {
			super(ACClientProps.TEX_EFF_RAILGUN);
			width = 0.12;
		}
		
		@Override
		protected void drawAtOrigin(EntityRailgun ent, double len) {
			System.out.println("rl" + len);
			super.drawAtOrigin(ent, len);
		}
		
	}
	
	int explRadius;
	float damage;
	
	boolean gened = false;
	
	public EntityRailgun(AbilityData data) {
		super(data.getPlayer());
		this.follow = false;
		this.unfoldTime = 200;
		
		int sid = data.getSkillID(SkillRailgun.instance);
		damage = 19 + data.getSkillLevel(sid) * 3F + (data.getLevelID() - 3) * 13F;
		explRadius = (int) (damage * 0.04);
		
		if(!worldObj.isRemote) {
			MovingObjectPosition ret = this.peformTrace(GenericUtils.selectorLiving);
			if(ret != null) onCollide(ret);
		}
		this.syncTrick = false;
	}
	
	private void genArcs() {
		double maxDist = 20;
		int n = 10 + rand.nextInt(4);
		double step = maxDist / n;
		
		final double rfr = -0.2, rto = 0.2;
		
		Motion3D mo = new Motion3D(this, false);
		for(int i = 0; i < n; ++i) {
			double x = mo.posX + GenericUtils.randIntv(rfr, rto), 
					y = mo.posY + GenericUtils.randIntv(rfr, rto) - 0.3, 
					z = mo.posZ + GenericUtils.randIntv(rfr, rto);
			EntityArcS arc = EntityArcS.get(worldObj);
			arc.setPosition(x, y, z);
			arc.addDaemonHandler(new LifeTime(arc, 15));
			worldObj.spawnEntityInWorld(arc);
			mo.move(step);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public EntityRailgun(World world) {
		super(world);
		syncTrick = false;
		peformTrace = true;
	}
	
	private void onCollide(MovingObjectPosition trace) {
		Explosion exp = worldObj.createExplosion(this.getThrower(), trace.hitVec.xCoord, trace.hitVec.yCoord, trace.hitVec.zCoord, explRadius, true);
		exp.doExplosionA();
		exp.doExplosionB(false);
	}
	
	@Override
	public void onUpdate() {
		if(ticksExisted == 8) {
			this.setFadeout(4);
		}
		
		if(worldObj.isRemote && !gened) {
			gened = true;
			genArcs();
		}
		
		//System.out.println(worldObj.isRemote + " " + traceDist);
		
		super.onUpdate();
	}
	
	@Override
	public double getMaxDistance() {
		return 100.0;
	}
	
	@Override
	public double getMinDistance() {
		return 30.0;
	}
	
}
