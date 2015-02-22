/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.academy.ability.electro.entity.fx.ChargeEffectS;
import cn.academy.energy.util.EnergyUtils;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The arc that automatically charges the rayTraced block each tick, if fail, (possibly) damage the player
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity
public class EntityChargingArc extends EntityArcBase {
	
	final int ept; //charge per tick
	
	@SideOnly(Side.CLIENT)
	ChargeEffectS eff;

	public EntityChargingArc(EntityPlayer creator, int _ept) {
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
		MovingObjectPosition res = performTrace();
		if(res != null) {
			if(!worldObj.isRemote) {
				if(EnergyUtils.isElecBlock(worldObj, res.blockX, res.blockY, res.blockZ)) {
					EnergyUtils.tryCharge(worldObj, res.blockX, res.blockY, res.blockZ, ept);
				}
			} else {
				if(eff == null) {
					worldObj.spawnEntityInWorld(eff = new ChargeEffectS(worldObj, res.blockX + .5, res.blockY + 2, res.blockZ + .5, 10000, 6, 1));
				}
				if(EnergyUtils.isElecBlock(worldObj, res.blockX, res.blockY, res.blockZ)) {
					eff.setPosition(res.blockX + .5, res.blockY + 2, res.blockZ + .5);
					eff.draw = true;
				} else eff.draw = false;
			}
		}
	}
	
	@Override
	public void setDead() {
		super.setDead();
		if(worldObj.isRemote && eff != null) {
			eff.setDead();
		}
	}
	
	@Override
	public MovingObjectPosition performTrace() {
		Motion3D mo = new Motion3D(this, true);
		Vec3 v1 = mo.getPosVec(worldObj), v2 = mo.move(getDefaultRayLen()).getPosVec(worldObj);
		return worldObj.rayTraceBlocks(v1, v2);
	}

}
