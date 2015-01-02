/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.weaponmod.api.damage.Damage;

/**
 * The railgun ray entity, alive on both client and server side.
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity
public class EntityRailgun extends EntityRay {

	private class RailgunDamage extends Damage {
		
		float power;

		public RailgunDamage(EntityPlayer elb, float power) {
			super(DamageSource.causePlayerDamage(elb));
			this.power = power;
		}
		
		public void damageEntity(Entity ent) {
			super.damageEntity(ent);
			explode(ent.posX, ent.posY, ent.posZ);
			//BUFF or something?
		}

		@Override
		protected float getDamage(Entity ent) {
			return (float) (power * 0.8);
		}
		
	}
	
	float power;

	public EntityRailgun(EntityPlayer elb, float pow) {
		super(elb, null);
		this.damage = new RailgunDamage(elb, pow);
		power = pow;
		setVelocity(10F);
	}

	public EntityRailgun(World world) {
		super(world);
		setVelocity(9F);
	}
	
	@Override
	protected void onBlockHit(int x, int y, int z) {
		if(worldObj.isRemote) return;
		explode(x + 0.5, y + 0.5, z + 0.5);
	}
	
	private void explode(double x, double y, double z) {
		worldObj.createExplosion(this, x, y, z, power * 4, true);
	}

}
