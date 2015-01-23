/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cn.academy.ability.electro.skill.SkillArcGen;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.MotionHandler;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
public class EntityAttackingArc extends EntityArcBase {
	
	float dmg;
	double igniteProb;

	public EntityAttackingArc(EntityPlayer creator, SkillArcGen sag) {
		super(creator);
		AbilityData data = AbilityDataMain.getData(creator);
		int skillID = data.getSkillID(sag);
		dmg = 3 + data.getSkillLevel(skillID) * 0.5F + data.getLevelID() + 1;
		igniteProb = 0.1 + 0.03 * data.getSkillLevel(skillID) + data.getLevelID() * 0.05;
		this.addDaemonHandler(new MotionHandler(this) {
			@Override
			public void onCreated() {
				MovingObjectPosition mop = peformTrace();
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
			@Override
			public void onUpdate() {}
			@Override
			public String getID() {
				return "Attack";
			}
			
		});
		lifeTime = 8;
	}

	public EntityAttackingArc(World world) {
		super(world);
		randomDraw = false;
	}

}
