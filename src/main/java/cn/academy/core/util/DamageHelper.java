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
package cn.academy.core.util;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.mc.WorldUtils;

/**
 * @author WeAthFolD
 */
public class DamageHelper {
	
	/**
	 * Apply a range attack on a specific point and range. The damage attenuates linearly. 
	 * At the center, the damage is [damage], at the edge the damage is 0.
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param range
	 * @param damage
	 * @param dmgSrc
	 * @param entitySelector
	 */
	public static void applyRangeAttack(
		World world, double x, double y, double z, double range, 
		float damage, DamageSource dmgSrc, 
		IEntitySelector entitySelector) {
		List<Entity> list = WorldUtils.getEntities(world, x, y, z, range, entitySelector);
		for(Entity ent : list) {
			double dist = MathUtils.distance(x, y, z, ent.posX, ent.posY, ent.posZ);
			float factor = 1 - MathUtils.wrapf(0, 1, (float) (dist / range));
			float appliedDamage = MathUtils.lerpf(0, damage, factor);
			System.out.println(ent + ", " + appliedDamage);
			ent.attackEntityFrom(dmgSrc, appliedDamage);
		}
	}
	
}
