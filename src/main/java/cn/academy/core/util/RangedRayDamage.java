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

import static cn.liutils.util.generic.VecUtils.add;
import static cn.liutils.util.generic.VecUtils.multiply;
import static cn.liutils.util.generic.VecUtils.subtract;
import static cn.liutils.util.generic.VecUtils.vec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cn.academy.core.event.BlockDestroyEvent;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Motion3D;
import cn.liutils.util.mc.EntitySelectors;
import cn.liutils.util.mc.WorldUtils;

/**
 * A super boomy ranged ray damage. it starts out a ranged ray in the given position and direction,
 * 	and destroy blocks in the path, also damages entities. It takes account of global damage switches.
 * @author WeAthFolD
 */
public class RangedRayDamage {
	
	static final double STEP = 0.9;

	public final World world;
	public final Motion3D motion;
	public double range;
	public float totalEnergy; // decrements [hardness of block] when hit a block
	public int maxIncrement = 50;
	public float dropProb = 0.05f;
	
	public IEntitySelector entitySelector = EntitySelectors.everything;
	public DamageSource dmgSrc = DamageSource.generic;
	public float startDamage = 10.0f; // ATTN: LINEAR 1.0*startDamage at dist 0; 0.2 * startDamage at maxIncrement
	
	private Vec3 start, slope;
	
	public RangedRayDamage(World _world, Motion3D _motion, double _range, float _energy) {
		world = _world;
		motion = _motion;
		range = _range;
		totalEnergy = _energy;
	}
	
	public RangedRayDamage(Entity entity, double _range, float _energy) {
		this(entity.worldObj, new Motion3D(entity, true).move(0.3), _range, _energy);
		if(entity instanceof EntityLivingBase) {
			dmgSrc = DamageSource.causeMobDamage((EntityLivingBase) entity);
		}
		entitySelector = EntitySelectors.excludeOf(entity);
	}
	
	/**
	 * BOOM!
	 */
	public void perform() {
		motion.normalize();
		Set<int[]> processed = new HashSet();
		
		float yaw = -MathUtils.PI_F * 0.5f - motion.getRotationYawRadians(), 
				pitch = motion.getRotationPitchRadians();
		
		start = motion.getPosVec();
		slope = motion.getMotionVec();
		
		Vec3 vp0 = VecUtils.vec(0, 0, 1);
		vp0.rotateAroundZ(pitch);
		vp0.rotateAroundY(yaw);
		
		Vec3 vp1 = VecUtils.vec(0, 1, 0);
		vp1.rotateAroundZ(pitch);
		vp1.rotateAroundY(yaw);
		
		
		if(DamageHelper.DESTROY_BLOCKS) {
			for(double s = -range; s <= range; s += STEP) {
				for(double t = -range; t <= range; t += STEP) {
					double rr = range * RandUtils.ranged(0.9, 1.1);
					if(s * s + t * t > rr * rr)
						continue;
					
					Vec3 pos = VecUtils.add(start, 
						VecUtils.add(
							VecUtils.multiply(vp0, s),
							VecUtils.multiply(vp1, t)));
					
					int[] coords = { (int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord };
					if(processed.contains(coords))
						continue;
					
					processed.add(coords);
				}
			}
			
			float ave = totalEnergy / processed.size();
			for(int[] coords : processed) {
				processLine(coords[0], coords[1], coords[2], 
					slope, ave * RandUtils.rangef(0.95f, 1.05f));
			}
		}
		
		/* Apply Entity Damage */ {
			Vec3 v0 = add(start, add(multiply(vp0, -range), multiply(vp1, -range))),
				v1 = add(start, add(multiply(vp0, range), multiply(vp1, -range))),
				v2 = add(start, add(multiply(vp0, range), multiply(vp1, range))),
				v3 = add(start, add(multiply(vp0, -range), multiply(vp1, range))),
				v4 = add(v0, multiply(slope, maxIncrement)),
				v5 = add(v1, multiply(slope, maxIncrement)),
				v6 = add(v2, multiply(slope, maxIncrement)),
				v7 = add(v3, multiply(slope, maxIncrement));
			AxisAlignedBB aabb = WorldUtils.ofPoints(v0, v1, v2, v3, v4, v5, v6, v7);
			
			IEntitySelector areaSelector = new IEntitySelector() {

				@Override
				public boolean isEntityApplicable(Entity target) {
					Vec3 dv = subtract(vec(target.posX, target.posY, target.posZ), start);
					Vec3 proj = dv.crossProduct(slope);
					return proj.lengthVector() < range * 1.2;
				}
				
			};
			List<Entity> targets = WorldUtils.getEntities(world, aabb, EntitySelectors.combine(entitySelector, areaSelector));
			for(Entity e : targets) {
				attackEntity(e);
			}
		}
	}
	
	private void processLine(int x0, int y0, int z0, Vec3 slope, float energy) {
		Plotter plotter = new Plotter(x0, y0, z0, slope.xCoord, slope.yCoord, slope.zCoord);
		int incrs = 0;
		for(int i = 0; i <= maxIncrement && energy > 0; ++i) {
			++incrs;
			int[] coords = plotter.next();
			int x = coords[0], y = coords[1], z = coords[2];
			boolean snd = incrs < 20;
			
			energy = destroyBlock(energy, x, y, z, snd);
			
			if(RandUtils.ranged(0, 1) < 0.05) {
				ForgeDirection dir = ForgeDirection.values()[RandUtils.rangei(0, 6)];
				energy = destroyBlock(energy, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, snd);
			}
		}
		//System.out.println(String.format("%d increments", incrs));
	}
	
	private float destroyBlock(float energy, int x, int y, int z, boolean snd) {
		Block block = world.getBlock(x, y, z);
		float hardness = block.getBlockHardness(world, x, y, z);
		if(hardness < 0)
			hardness = 233333;
		if(!MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, x, y, z)) && energy >= hardness) {
			if(block.getMaterial() != Material.air) {
				block.dropBlockAsItemWithChance(world, x, y, z, 
					world.getBlockMetadata(x, y, z), dropProb, 0);
				
				if(snd && RandUtils.ranged(0, 1) < 0.1) {
					world.playSoundEffect(x + 0.5F, y + 0.5F, 
	                		z + 0.5F, 
	                		block.stepSound.getBreakSound(), 
	                		(block.stepSound.getVolume() + 1.0F) / 2.0F, 
	                		block.stepSound.getPitch());
				}
			}
			world.setBlockToAir(x, y, z);
			return energy - hardness;
		}
		return 0;
	}
	
	private void attackEntity(Entity target) {
		Vec3 dv = subtract(vec(target.posX, target.posY, target.posZ), start);
		float dist = Math.min(maxIncrement, (float) dv.crossProduct(slope).lengthVector());
		
		float realDmg = this.startDamage * MathUtils.lerpf(1, 0.2f, dist / maxIncrement);
		DamageHelper.attack(target, dmgSrc, realDmg);
	}
	
	static class Plotter {
		
		enum Axis { X, Y, Z };
		
		final Axis axis; // The main axis which we incr(decr) through
		final double dyx, dzx;
		final int dirflag;
		int x0, y0, z0;
		
		int x, y, z;
		
		Plotter(int _x0, int _y0, int _z0, 
				double dx, double dy, double dz) {
			x0 = _x0;
			y0 = _y0;
			z0 = _z0;
			
			// Determine which direction to increment to
			// and swap the values so we are always thinking we are incrementing via x+
			double adx = Math.abs(dx), ady = Math.abs(dy), adz = Math.abs(dz);
			
			int itemp;
			double dtemp;
			if(adz > ady && adz > adx) {
				dtemp = dz; dz = dx; dx = dtemp;
				itemp = z0; z0 = x0; x0 = itemp;
				axis = Axis.Z;
			} else if(ady > adx) {
				dtemp = dy; dy = dx; dx = dtemp;
				itemp = y0; y0 = x0; x0 = itemp;
				axis = Axis.Y;
			} else if(adx > 0) {
				axis = Axis.X;
			} else
				throw new RuntimeException("Zero slope vector");
			
			x = x0;
			y = y0;
			z = z0;
			dyx = dy / dx;
			dzx = dz / dx;
			dirflag = dx > 0 ? 1 : -1;
		}
		
		int[] next() {
			int nextX = x + dirflag;
			double valy = y0 + (nextX - x0) * dyx;
			double valz = z0 + (nextX - x0) * dzx;
			if(Math.abs(valy - y) > 0.5) {
				// 卧槽见鬼了 += double不用cast
				y += Math.signum(dyx) * dirflag;
			} else if(Math.abs(valz - z) > 0.5) {
				z += Math.signum(dzx) * dirflag;
			} else {
				x = nextX;
			}
			
			switch(axis) {
			case X:
				return new int[] { x, y, z };
			case Y:
				return new int[] { y, x, z };
			case Z:
				return new int[] { z, y, x };
			}
			return null;
		}
		
	}
	
}
