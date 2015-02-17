/**
 * 
 */
package cn.academy.ability.meltdowner.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cn.academy.ability.meltdowner.entity.EntityMdRayBase.RayRender;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity()
@RegEntity.HasRender
public class EntityMeltDowner extends EntityMdRayBase {
	
	float dmg;

	public EntityMeltDowner(EntityLivingBase _spawner, float dmg) {
		super(_spawner);
		this.dmg = dmg;
	}

	@SideOnly(Side.CLIENT)
	public EntityMeltDowner(World world) {
		super(world);
	}

	@Override
	protected void handleCollision(MovingObjectPosition mop) {
		if(mop.typeOfHit == MovingObjectType.ENTITY) {
			mop.entityHit.attackEntityFrom(DamageSource.causeMobDamage(getSpawner()), dmg);
		}
	}

}
