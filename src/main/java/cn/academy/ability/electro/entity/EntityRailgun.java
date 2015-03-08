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
package cn.academy.ability.electro.entity;

import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.client.render.entity.RenderRailgun;
import cn.academy.ability.electro.entity.fx.EntityArcS;
import cn.academy.api.data.AbilityData;
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
	public static RenderRailgun renderer;
	
	int explRadius;
	float damage;
	
	boolean gened = false;
	
	public EntityRailgun(AbilityData data) {
		super(data.getPlayer());
		
		int sid = data.getSkillID(CatElectro.railgun);
		damage = Math.max(9, 19 + data.getSkillLevel(sid) * 3F + (data.getLevelID() - 3) * 13F);
		explRadius = (int) (damage * 0.2);
		
		MovingObjectPosition ret = this.performTrace();
		if(ret != null) onCollide(ret);
	}
	
	private void genArcs() {
		double maxDist = 20;
		int n = 10 + rand.nextInt(4);
		double step = maxDist / n;
		
		final double rfr = -0.2, rto = 0.2;
		
		Motion3D mo = new Motion3D(this, true);
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
	}
	
	private void onCollide(MovingObjectPosition trace) {
		Explosion exp = worldObj.createExplosion(this.getSpawner(), trace.hitVec.xCoord, trace.hitVec.yCoord, trace.hitVec.zCoord, explRadius, true);
		exp.doExplosionA();
		GenericUtils.doRangeDamage(worldObj, DamageSource.causeMobDamage(getSpawner()), 
			worldObj.getWorldVec3Pool().getVecFromPool(trace.hitVec.xCoord, trace.hitVec.yCoord, trace.hitVec.zCoord),
			damage * .4f,
			damage * .3);
		exp.doExplosionB(false);
	}
	
	@Override
	public boolean doesFollowSpawner() {
		return false;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(ticksExisted == 18) {
			this.setFadeout(8);
		}
		if(worldObj.isRemote && !gened && isLoaded()) {
			gened = true;
			genArcs();
		}
	}
	
	@Override
	public float getDefaultRayLen() {
		return 50.0f;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void beforeRender() { 
		//EntityLivingBase spawner = getSpawner();
		//setPosition(spawner.posX, spawner.posY, spawner.posZ);
	}
	
	@Override
	public float getDisplayRayLen() {
		float ratio = Math.min(1.0f, ticksExisted / 3.0f);
		float len = Math.max(getRayLength(), 12.0f);
		return ratio * len;
	}
	
}
