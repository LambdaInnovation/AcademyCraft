package cn.academy.misc.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.register.Configurable;
import cn.liutils.registry.ConfigurableRegistry.RegConfigurable;
import cn.liutils.util.GenericUtils;

/**
 * Process gateway that filters the player attack event/explosion so that they can be modified by config settings.
 * All explosion/damage done in skills should delegate to this class.
 * @author WeathFolD
 */
@RegistrationClass
@RegConfigurable
public class DamageHelper {
	
	@Configurable(key = "damagePlayer", category = "general", 
		defValueBool = true, comment = "Enable damage on players when using abilities.")
	public static boolean attackPlayer;
	
	@Configurable(key = "destroyTerrain", category = "general", 
			defValueBool = true, comment = "Enable terrain destruction of abilities.")
	public static boolean destroyTerrain;

	public static void applyEntityDamage(Entity e, DamageSource ds, float dmg) {
		if(ds.getEntity() instanceof EntityPlayer) {
			if(!attackPlayer)
				return;
		}
		
		e.attackEntityFrom(ds, dmg);
	}
	
	public static void explode(World world, Entity entity, float strengh,
			double radius, double posX, double posY, double posZ, float adddmg) {
		System.out.println(destroyTerrain);
		GenericUtils.explode(world, entity, strengh, radius, posX, posY, posZ, adddmg, destroyTerrain);
	}

}
