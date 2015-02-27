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
package cn.academy.ability.electro.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.entity.fx.EntityExcitedArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public abstract class AttackingArcBase extends EntityArcBase {

	public AttackingArcBase(EntityPlayer creator) {
		super(creator);
		AbilityData data = AbilityDataMain.getData(creator);
		doAttack(data);
		addDaemonHandler(new LifeTime(this, getLifetime()));
	}

	public AttackingArcBase(World world) {
		super(world);
	}

	private void doAttack(AbilityData data) {
		int sid = getSkill().getIndexInCategory(CatElectro.INSTANCE);
		int slv = data.getSkillLevel(sid), lv = data.getLevelID() + 1;
		
		MovingObjectPosition mop = performTrace();
		float damage = getDamage(slv, lv);
		if(mop != null) {
			if(mop.typeOfHit == MovingObjectType.ENTITY) {
				mop.entityHit.attackEntityFrom(DamageSource.causePlayerDamage(data.getPlayer()),
					damage);
			} else {
				double ip = getIgniteProb(slv, lv);
				if(rand.nextDouble() <= ip) {
					if(worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ) == Blocks.air) {
						worldObj.setBlock(mop.blockX, mop.blockY + 1, mop.blockZ, Blocks.fire);
					}
				}
			}
			double range = getAOERange(slv, lv);
			//Start searching entities and do range damage
			List<Entity> ents = EntityUtils.getEntitiesAround
				(worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 
				range, GenericUtils.selectorLiving, this, getSpawner());
			float sdmg = damage * 0.4f;
			double tx = mop.hitVec.xCoord, ty = mop.hitVec.yCoord, tz = mop.hitVec.zCoord;
			for(Entity ent : ents) {
				//spawn the arc and attack the AOEed target
				if(ent.equals(getSpawner()))
					continue;
				ent.attackEntityFrom(DamageSource.causeMobDamage(getSpawner()), sdmg);
				double ox = ent.posX, oy = ent.posY + ent.height * 0.6, oz = ent.posZ;
				new EntityExcitedArc(worldObj, 
					Vec3.createVectorHelper(tx, ty, tz),
					Vec3.createVectorHelper(ox, oy, oz), 5);
			}
		}
	}
	
	protected abstract SkillBase getSkill();
	
	protected abstract float getDamage(int slv, int lv);
	
	protected abstract double getAOERange(int slv, int lv);
	
	protected abstract double getIgniteProb(int slv, int lv);
	
	protected abstract int getLifetime();
	
	@SideOnly(Side.CLIENT)
	@Override
	public abstract ResourceLocation[] getTexs();
	
	@Override
	public boolean doesFollowSpawner() {
		return true;
	}
	
}
