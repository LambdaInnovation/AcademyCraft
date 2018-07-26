package cn.academy.crafting.world;

import cn.academy.crafting.ModuleCrafting;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * AC Ore Generator
 * @author KSkun
 */
public class ACWorldGen implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
            IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if(ModuleCrafting.GENERATE_ORES && world.provider.getDimensionName().equals("Overworld")) {
            genOverworld(world, random, chunkX * 16, chunkZ * 16);
        }
    }
    
    private List<CustomWorldGen> generators = Arrays.asList(new CustomWorldGen[] {
            new CustomWorldGen(new WorldGenMinable(ModuleCrafting.oreResoCrystal, 0, 4, Blocks.stone), 60, 18),
            new CustomWorldGen(new WorldGenMinable(ModuleCrafting.oreConstraintMetal, 0, 4, Blocks.stone), 60, 24),
            new CustomWorldGen(new WorldGenMinable(ModuleCrafting.oreImagCrystal, 0, 3, Blocks.stone), 60, 48),
            new CustomWorldGen(new WorldGenMinable(ModuleCrafting.oreImagSil, 0, 4, Blocks.stone), 60, 22)
    });
    
    private void genOverworld(World world, Random random, int x, int z) {
        for(CustomWorldGen gen : generators) {
            gen.generate(world, random, x, z);
        }
    }

}