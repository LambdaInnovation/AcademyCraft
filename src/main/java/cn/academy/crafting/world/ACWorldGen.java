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
package cn.academy.crafting.world;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cn.academy.crafting.ModuleCrafting;
import net.minecraft.init.Blocks;
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
        if(ModuleCrafting.GENERATE_ORES) {
            switch(world.provider.dimensionId) {
            case 0:
                genOverworld(world, random, chunkX * 16, chunkZ * 16);
                break;
            }
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