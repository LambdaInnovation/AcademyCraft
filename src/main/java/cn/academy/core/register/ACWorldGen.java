package cn.academy.core.register;

import java.util.Random;

import net.minecraft.world.World;
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
			genOverworld(world, random, chunkX, chunkZ);
			break;
		}
	}
	
	public void genOverworld(World world, Random random, int i, int j) {
		for (int k = 0; k < 35; k++) {
			int tinOreXCoord = i + random.nextInt(16);
			int tinOreYCoord = random.nextInt(48);
			int tinOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreTin, 3)).generate(world, random, 
					tinOreXCoord, tinOreYCoord, tinOreZCoord);
		}
		for (int k = 0; k < 35; k++) {
			int copperOreXCoord = i + random.nextInt(16);
			int copperOreYCoord = random.nextInt(48);
			int copperOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreCopper, 5)).generate(world, random, 
					copperOreXCoord, copperOreYCoord, copperOreZCoord);
		}
		for (int k = 0; k < 38; k++) {
			int alOreXCoord = i + random.nextInt(16);
			int alOreYCoord = random.nextInt(48);
			int alOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreAl, 5)).generate(world, random, 
					alOreXCoord, alOreYCoord, alOreZCoord);
		}
		for (int k = 0; k < 35; k++) {
			int mgOreXCoord = i + random.nextInt(16);
			int mgOreYCoord = random.nextInt(48);
			int mgOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreMg, 3)).generate(world, random, 
					mgOreXCoord, mgOreYCoord, mgOreZCoord);
		}
		for (int k = 0; k < 30; k++) {
			int niOreXCoord = i + random.nextInt(16);
			int niOreYCoord = random.nextInt(48);
			int niOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreNi, 3)).generate(world, random, 
					niOreXCoord, niOreYCoord, niOreZCoord);
		}
		//TODO: Follow the document to finish these ores' gen.
/*		for (int k = 0; k < 10; k++) {
			int cryOreXCoord = i + random.nextInt(16);
			int cryOreYCoord = random.nextInt(20);
			int cryOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreCrystal, 2)).generate(world, random, 
					cryOreXCoord, cryOreYCoord, cryOreZCoord);
		}
		for (int k = 0; k < 2; k++) {
			int shadowOreXCoord = i + random.nextInt(16);
			int shadowOreYCoord = random.nextInt(10);
			int shadowOreZCoord = j + random.nextInt(16);
			(new WorldGenMinable(ACBlocks.oreCrystal, 1)).generate(world, random, 
					shadowOreXCoord, shadowOreYCoord, shadowOreZCoord);
		}*/
	}

}
