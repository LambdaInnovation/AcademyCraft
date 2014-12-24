/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.liutils.api.entity.EntityBullet;

/**
 * @author WeathFolD
 *
 */
public class EntityArcBullet extends EntityBullet {

	/**
	 * @param par1World
	 * @param par2EntityLiving
	 * @param dmg
	 */
	public EntityArcBullet(World par1World, EntityLivingBase par2EntityLiving,
			float dmg) {
		super(par1World, par2EntityLiving, dmg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param par1World
	 * @param ent
	 * @param dmg
	 * @param scatterRadius
	 */
	public EntityArcBullet(World par1World, EntityLivingBase ent, float dmg,
			float scatterRadius) {
		super(par1World, ent, dmg, scatterRadius);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param par1World
	 * @param par2EntityLiving
	 * @param dmg
	 * @param rev
	 */
	public EntityArcBullet(World par1World, EntityLivingBase par2EntityLiving,
			float dmg, boolean rev) {
		super(par1World, par2EntityLiving, dmg, rev);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param par1World
	 * @param ent
	 * @param target
	 * @param dmg
	 * @param oriHgt
	 * @param targHgt
	 */
	public EntityArcBullet(World par1World, Entity ent, Entity target,
			float dmg, float oriHgt, float targHgt) {
		super(par1World, ent, target, dmg, oriHgt, targHgt);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param par1World
	 * @param ent
	 * @param target
	 * @param dmg
	 */
	public EntityArcBullet(World par1World, EntityLivingBase ent,
			Entity target, float dmg) {
		super(par1World, ent, target, dmg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param world
	 * @param begin
	 * @param motion
	 * @param dmg
	 */
	public EntityArcBullet(World world, Vec3 begin, Vec3 motion, float dmg) {
		super(world, begin, motion, dmg);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param world
	 */
	public EntityArcBullet(World world) {
		super(world);
		// TODO Auto-generated constructor stub
	}

}
