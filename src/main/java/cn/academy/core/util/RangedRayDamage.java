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

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Motion3D;

/**
 * A super boomy ranged ray damage. it starts out a ranged ray in the given position and direction,
 * 	and destroy blocks in the path, also damages entities. It takes account of global damage switches.
 * 
 * TODO Implement entity damage
 * TODO Implement energy distribution
 * TODO Render effects
 * @author WeAthFolD
 */
public class RangedRayDamage {
	
	static final double STEP = 0.9;

	public final World world;
	public final Motion3D motion;
	public double range;
	public float totalEnergy; // decrements [hardness of block] when hit a block
	public int maxIncrement = 100;
	
	public RangedRayDamage(World _world, Motion3D _motion, double _range, float _energy) {
		world = _world;
		motion = _motion;
		range = _range;
		totalEnergy = _energy;
	}
	
	public RangedRayDamage(Entity entity, double _range, float _energy) {
		this(entity.worldObj, new Motion3D(entity, true).move(1), _range, _energy);
	}
	
	/**
	 * BOOM!
	 */
	public void perform() {
		// Plot the line
		motion.normalize();
		Set<int[]> processed = new HashSet();
		
		float yaw = -MathUtils.PI_F * 0.5f - motion.getRotationYawRadians(), 
				pitch = motion.getRotationPitchRadians();
		
		Vec3 start = motion.getPosVec(), slope = motion.getMotionVec();
		Vec3 vp0 = VecUtils.vec(0, 0, 1);
		vp0.rotateAroundZ(pitch);
		vp0.rotateAroundY(yaw);
		
		Vec3 vp1 = VecUtils.vec(0, 1, 0);
		vp1.rotateAroundZ(pitch);
		vp1.rotateAroundY(yaw);
		
		
		if(DamageHelper.DESTROY_BLOCKS) {
			for(double s = -range; s <= range; s += STEP) {
				for(double t = (int) -range; t <= range; t += STEP) {
					if(s * s + t * t > range * range)
						continue;
					
					Vec3 pos = VecUtils.add(start, 
						VecUtils.add(
							VecUtils.scalarMultiply(vp0, s),
							VecUtils.scalarMultiply(vp1, t)));
					
					int[] coords = { (int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord };
					if(processed.contains(coords))
						continue;
					
					processed.add(coords);
					processLine(coords[0], coords[1], coords[2], slope, 1000);
				}
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
			
			Block block = world.getBlock(x, y, z);
			float hardness = block.getBlockHardness(world, x, y, z);
			if(hardness < 0) hardness = 2333333f;
			if((energy -= hardness) > 0) {
				//TODO Add destroy effects
				world.setBlockToAir(x, y, z);
			}
//			world.setBlock(x, y, z, Blocks.stone);
		}
		System.out.println(String.format("%d increments", incrs));
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
			if(Math.abs(valy - y) > 1.0) {
				y += Math.signum(dyx) * dirflag;
			} else if(valz - z > 1.0) {
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
