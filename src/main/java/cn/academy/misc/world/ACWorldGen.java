package cn.academy.misc.world;

import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.Random;

import cn.academy.core.register.ACBlocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import cpw.mods.fml.common.IWorldGenerator;

/**
 * AC Ore Generator
 * @author KSkun
 */
public class ACWorldGen implements IWorldGenerator {

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		switch(world.provider.dimensionId) {
		case 0:
			genOverworld(world, random, chunkX * 16, chunkZ * 16);
			break;
		}
	}
	
	public void genOverworld(World world, Random random, int x, int z) {
		WorldChunkManager chunkmgr = new WorldChunkManager(world);
		int biomeId = chunkmgr.getBiomeGenAt(x, z).biomeID;
		//TODO: This ore gen isn't available in this version.
/*		for (int k = 0; k < 35; ++k) {
			int xc = x + random.nextInt(16);
			int yc = random.nextInt(48);
			int zc = z + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreTxn, 3)).generate(world, random, 
					xc, yc, zc);
		}*/
		for (int k = 0; k < 40; ++k) {
			int xc = x + random.nextInt(16);
			int yc = random.nextInt(50);
			int zc = z + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreCopper, 7)).generate(world, random, 
					xc, yc, zc);
		}
		for (int k = 0; k < 40; ++k) {
			int xc = x + random.nextInt(16);
			int yc = random.nextInt(50);
			int zc = z + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreAl, 5)).generate(world, random, 
					xc, yc, zc);
		}
		for (int k = 0; k < 30; ++k) {
			int xc = x + random.nextInt(16);
			int yc = random.nextInt(50);
			int zc = z + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreMg, 3)).generate(world, random, 
					xc, yc, zc);
		}
		for (int k = 0; k < 35; ++k) {
			int xc = x + random.nextInt(16);
			int yc = random.nextInt(50);
			int zc = z + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreNi, 3)).generate(world, random, 
					xc, yc, zc);
		}		
		switch(biomeId) {
		case 3:
		case 13:
		case 17:
		case 18:
		case 19:
		case 22:
		case 28:
		case 31:
		case 33:
		case 131:
		case 156:
		case 161:
		case 0:
		case 10:
		case 24:
			for (int k = 0; k < 2; ++k) {
				int xc = x + random.nextInt(16);
				int yc = random.nextInt(10);
				int zc = z + random.nextInt(16);
				(new WorldGenMinable(ACBlocks.oreShadow, 2)).generate(world, random, 
						xc, yc, zc);
			}
			break;
		}
		switch(biomeId) {
		case 3:
		case 13:
		case 17:
		case 18:
		case 19:
		case 22:
		case 28:
		case 31:
		case 33:
		case 131:
		case 156:
		case 161:
		case 2:
		case 130:
			for (int k = 0; k < 10; ++k) {
				int xc = x + random.nextInt(16);
				int yc = random.nextInt(30);
				int zc = z + random.nextInt(16);
				(new WorldGenMinable(ACBlocks.oreCrystal, 3)).generate(world, random, 
						xc, yc, zc);
			}
			break;
		}
	}

}
