/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.core.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.space.Motion3D;

/**
 * The arc that automatically charges the rayTraced block each tick, if fail, (possibly) damage the player
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity
public class EntityChargingArc extends EntityArcBase {
	
	final int ept; //charge per tick

	public EntityChargingArc(EntityLivingBase creator, int _ept) {
		super(creator);
		ept = _ept;
	}

	public EntityChargingArc(World world) {
		super(world);
		ept = 0;
	}
	
	@Override
	public boolean doesFollowSpawner() {
		return true;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(!worldObj.isRemote) {
			MovingObjectPosition res = performTrace();
			if(res != null) {
				if(EnergyUtils.isElecBlock(worldObj, res.blockX, res.blockY, res.blockZ)) {
					EnergyUtils.tryCharge(worldObj, res.blockX, res.blockY, res.blockZ, ept);
				}
			}
		}
	}
	
	@Override
	public MovingObjectPosition performTrace() {
		Motion3D mo = new Motion3D(this, true);
		Vec3 v1 = mo.getPosVec(worldObj), v2 = mo.move(getDefaultRayLen()).getPosVec(worldObj);
		return worldObj.rayTraceBlocks(v1, v2);
	}

}
