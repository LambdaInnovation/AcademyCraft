package cn.academy.crafting;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenerator;
import cn.academy.energy.ModuleEnergy;

public class WorldGenPhaseLiq extends WorldGenerator {
	
    private Block theBlock;

    public WorldGenPhaseLiq() {
        this.theBlock = ModuleEnergy.imagPhase;
    }

    public boolean generate(World world, Random random, int x, int y, int z) {
        for (x -= 8, z -= 8; y > 5 && world.isAirBlock(x, y, z); --y); // Find a non-air-block as origin to generate.

        if (y <= 4)
            return false;
        
        y -= 4;
        boolean[] buffer = new boolean[2048];
        
        for (int i = 0, loops = random.nextInt(4) + 4; i < loops; ++i) {
            double d0 = random.nextDouble() * 6.0D + 3.0D; // 3~9
            double d1 = random.nextDouble() * 4.0D + 2.0D; // 2~6
            double d2 = random.nextDouble() * 6.0D + 3.0D; // 3~9
            
            double d3 = random.nextDouble() * (14.0D - d0) + 1.0D + d0 / 2.0D;
            double d4 = random.nextDouble() * (4.0D - d1) + 2.0D + d1 / 2.0D;
            double d5 = random.nextDouble() * (14.0D - d2) + 1.0D + d2 / 2.0D;

            for (int j = 1; j < 15; ++j)
            {
                for (int k = 1; k < 15; ++k)
                {
                    for (int l = 1; l < 7; ++l)
                    {
                        double d6 = ((double)j - d3) / (d0 / 2.0D);
                        double d7 = ((double)l - d4) / (d1 / 2.0D);
                        double d8 = ((double)k - d5) / (d2 / 2.0D);
                        double d9 = d6 * d6 + d7 * d7 + d8 * d8;

                        if (d9 < 0.6D)
                        {
                            buffer[(j * 16 + k) * 8 + l] = true;
                        }
                    }
                }
            }
        }

        boolean flag;

        for (int i = 0; i < 16; ++i) {
            for (int j2 = 0; j2 < 16; ++j2) {
                for (int j1 = 0; j1 < 8; ++j1) {
                    flag = !buffer[(i * 16 + j2) * 8 + j1] && (i < 15 && buffer[((i + 1) * 16 + j2) * 8 + j1] || i > 0 && buffer[((i - 1) * 16 + j2) * 8 + j1] || j2 < 15 && buffer[(i * 16 + j2 + 1) * 8 + j1] || j2 > 0 && buffer[(i * 16 + (j2 - 1)) * 8 + j1] || j1 < 7 && buffer[(i * 16 + j2) * 8 + j1 + 1] || j1 > 0 && buffer[(i * 16 + j2) * 8 + (j1 - 1)]);

                    if (flag)
                    {
                        Material material = world.getBlock(x + i, y + j1, z + j2).getMaterial();

                        if (j1 >= 4 && material.isLiquid())
                        {
                            return false;
                        }

                        if (j1 < 4 && !material.isSolid() && world.getBlock(x + i, y + j1, z + j2) != this.theBlock)
                        {
                            return false;
                        }
                    }
                }
            }
        }

        for (int i1 = 0; i1 < 16; ++i1) {
            for (int j2 = 0; j2 < 16; ++j2) {
                for (int j1 = 0; j1 < 8; ++j1) {
                    if (buffer[(i1 * 16 + j2) * 8 + j1]) {
                        world.setBlock(x + i1, y + j1, z + j2, j1 >= 4 ? Blocks.air : this.theBlock, 0, 2);
                    }
                }
            }
        }

        for (int i1 = 0; i1 < 16; ++i1) {
            for (int j2 = 0; j2 < 16; ++j2) {
                for (int j1 = 4; j1 < 8; ++j1) {
                	
                    if (buffer[(i1 * 16 + j2) * 8 + j1] && 
                    	world.getBlock(x + i1, y + j1 - 1, z + j2) == Blocks.dirt && 
                    	world.getSavedLightValue(EnumSkyBlock.Sky, x + i1, y + j1, z + j2) > 0) {
                        BiomeGenBase biomegenbase = world.getBiomeGenForCoords(x + i1, z + j2);

                        if (biomegenbase.topBlock == Blocks.mycelium)
                        {
                            world.setBlock(x + i1, y + j1 - 1, z + j2, Blocks.mycelium, 0, 2);
                        }
                        else
                        {
                            world.setBlock(x + i1, y + j1 - 1, z + j2, Blocks.grass, 0, 2);
                        }
                    }
                }
            }
        }

        for (int i1 = 0; i1 < 16; ++i1) {
            for (int j2 = 0; j2 < 16; ++j2) {
                if (world.isBlockFreezable(x + i1, y + 4, z + j2)) {
                    world.setBlock(x + i1, y + 4, z + j2, Blocks.ice, 0, 2);
                }
            }
        }

        return true;
    }
}