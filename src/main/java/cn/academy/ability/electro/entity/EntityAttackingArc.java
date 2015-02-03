/**
 * 
 */
package cn.academy.ability.electro.entity;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.entity.fx.EntityExcitedArc;
import cn.academy.ability.electro.skill.SkillArcGen;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.MotionHandler;
import cn.liutils.util.EntityUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
public class EntityAttackingArc extends EntityArcBase {
	
	public static class OffSync extends EntityAttackingArc {

		public OffSync(EntityPlayer creator, SkillArcGen sag) {
			super(creator, sag);
			addEffectUpdate();
		}
		
	}
	
	float dmg;
	double igniteProb;
	double aoeRange;

	public EntityAttackingArc(EntityPlayer creator, SkillArcGen sag) {
		super(creator);
		AbilityData data = AbilityDataMain.getData(creator);
		int skillID = data.getSkillID(sag);
		dmg = 3 + data.getSkillLevel(skillID) * 0.5F + data.getLevelID() + 1;
		igniteProb = 0.1 + 0.03 * data.getSkillLevel(skillID) + data.getLevelID() * 0.05;
		aoeRange = 6;
		
		if(isSync) {
			this.addDaemonHandler(new MotionHandler(this) {
				@Override
				public void onCreated() {
					MovingObjectPosition mop = peformTrace(GenericUtils.selectorLiving);
					if(mop == null) return;
					
					if(mop.typeOfHit == MovingObjectType.BLOCK) {
						if(rand.nextDouble() < igniteProb) {
							if(worldObj.isAirBlock(mop.blockX, mop.blockY + 1, mop.blockZ)) {
								worldObj.setBlock(mop.blockX, mop.blockY + 1, mop.blockZ, Blocks.fire);
							}
						}
					} else {
						mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getThrower()), dmg);
					}
				}
				
				private void createExcArc(Vec3 v1, Vec3 v2) {
					worldObj.spawnEntityInWorld(new EntityExcitedArc(worldObj, v1, v2, 10));
				}
				
				@Override
				public void onUpdate() {}
				@Override
				public String getID() {
					return "Attack";
				}
				
			});
		}
		
		if(worldObj.isRemote) {
			this.addDaemonHandler(new MotionHandler(this) {
				@Override
				public void onCreated() {
					MovingObjectPosition mop = peformTrace(GenericUtils.selectorLiving);
					System.out.println(mop);
					if(mop == null) return;
					List<Entity> list = EntityUtils.getEntitiesAround(worldObj, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, aoeRange, 
							GenericUtils.selectorLiving, EntityAttackingArc.this, getThrower(), mop.entityHit);
					System.out.println(list);
					for(Entity e : list) {
						createExcArc(mop.hitVec, worldObj.getWorldVec3Pool().getVecFromPool(e.posX, e.posY, e.posZ));
					}
				}
				@SideOnly(Side.CLIENT)
				private void createExcArc(Vec3 v1, Vec3 v2) {
					System.out.println("create " + worldObj.isRemote);
					worldObj.spawnEntityInWorld(new EntityExcitedArc(worldObj, v1, v2, 10));
				}
				@Override
				public void onUpdate() {}
				@Override
				public String getID() {
					return "Attack";
				}
				
			});
		}
		lifeTime = 8;
	}

	public EntityAttackingArc(World world) {
		super(world);
		randomDraw = false;
	}

}
