package cn.academy.worldgen;

import cn.academy.ACBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

public class WorldGenPhaseLiq extends WorldGenerator {

    private Block theBlock;

    public WorldGenPhaseLiq() {
        this.theBlock = ACBlocks.imag_phase;
    }

    @Override
    public boolean generate(World world, Random random, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (x -= 8, z -= 8; y > 5 && world.isAirBlock(new BlockPos(x, y, z)); --y)
            ; // Find a non-air-block as origin to generate.

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

            for (int j = 1; j < 15; ++j) {
                for (int k = 1; k < 15; ++k) {
                    for (int l = 1; l < 7; ++l) {
                        double d6 = (j - d3) / (d0 / 2.0D);
                        double d7 = (l - d4) / (d1 / 2.0D);
                        double d8 = (k - d5) / (d2 / 2.0D);
                        double d9 = d6 * d6 + d7 * d7 + d8 * d8;

                        if (d9 < 0.6D) {
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
                    flag = !buffer[(i * 16 + j2) * 8 + j1] && (i < 15 && buffer[((i + 1) * 16 + j2) * 8 + j1]
                            || i > 0 && buffer[((i - 1) * 16 + j2) * 8 + j1]
                            || j2 < 15 && buffer[(i * 16 + j2 + 1) * 8 + j1]
                            || j2 > 0 && buffer[(i * 16 + (j2 - 1)) * 8 + j1]
                            || j1 < 7 && buffer[(i * 16 + j2) * 8 + j1 + 1]
                            || j1 > 0 && buffer[(i * 16 + j2) * 8 + (j1 - 1)]);

                    if (flag) {
                        BlockPos bpos = new BlockPos(x+i, y+ j1, z + j2);
                        IBlockState ibs = world.getBlockState(bpos);
                        Material material = ibs.getMaterial();

                        if (j1 >= 4 && material.isLiquid()) {
                            return false;
                        }

                        if (j1 < 4 && !material.isSolid() && ibs.getBlock() != this.theBlock) {
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
                        world.setBlockState(new BlockPos(x + i1, y + j1, z + j2),
                                j1 >= 4 ? Blocks.AIR.getDefaultState() : this.theBlock.getDefaultState(), 2);
                    }
                }
            }
        }

        for (int i1 = 0; i1 < 16; ++i1) {
            for (int j2 = 0; j2 < 16; ++j2) {
                for (int j1 = 4; j1 < 8; ++j1) {
                    BlockPos bPos = new BlockPos(x + i1, y + j1 - 1, z + j2);
                    IBlockState ibs = world.getBlockState(bPos);
                    if (buffer[(i1 * 16 + j2) * 8 + j1] && ibs.getBlock() == Blocks.DIRT
                            && ibs.getBlock().getLightValue(ibs, world, new BlockPos(x + i1, y + j1, z + j2)) > 0) {
                        Biome biomegenbase = world.getBiomeProvider().getBiome(bPos);

                        if (biomegenbase.topBlock == Blocks.MYCELIUM) {
                            world.setBlockState(bPos, Blocks.MYCELIUM.getDefaultState(), 2);
                        } else {
                            world.setBlockState(bPos, Blocks.GRASS.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        for (int i1 = 0; i1 < 16; ++i1) {
            for (int j2 = 0; j2 < 16; ++j2) {
                BlockPos bPos = new BlockPos(x + i1, y + 4, z + j2);
                if (world.canBlockFreezeWater(bPos)) {
                    world.setBlockState(bPos, Blocks.ICE.getDefaultState(), 2);
                }
            }
        }

        return true;
    }
}