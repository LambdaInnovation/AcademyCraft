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
package cn.academy.misc.world;

import java.util.Random;

import cn.academy.core.register.ACBlocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;

/**
 * AC Ore Generator
 * @author KSkun
 */
public class ACWorldGen implements IWorldGenerator {
	
	private int[] shadowGenIds = new int[] {0, 3, 10, 13, 17, 18, 19, 22, 24, 28, 31, 33, 131, 156, 161};

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		switch(world.provider.dimensionId) {
		case 0:
			genOverworld(world, random, chunkX * 16, chunkZ * 16);
			break;
		}
	}
	
	private void genOverworld(World world, Random random, int x, int z) {
		//TODO: This ore gen isn't available in this version.
//		(new ACGenMinable(ACBlocks.oreTin, 5, 60, 23)).generate(world, random, x, z);
		//TODO: Copper Ore is unavailable.
//		(new ACGenMinable(ACBlocks.oreCopper, 7, 60, 25)).generate(world, random, x, z);
		(new ACGenMinable(ACBlocks.oreAl, 5, 60, 25)).generate(world, random, x, z);
		(new ACGenMinable(ACBlocks.oreMg, 3, 60, 20)).generate(world, random, x, z);
		(new ACGenMinable(ACBlocks.oreNi, 3, 60, 18)).generate(world, random, x, z);
		(new ACGenMinable(ACBlocks.oreShadow, 2, 15, 5, shadowGenIds)).generate(world, random, x, z);
		(new ACGenMinable(ACBlocks.oreCrystal, 3, 30, 10)).generate(world, random, x, z);
	}

}
