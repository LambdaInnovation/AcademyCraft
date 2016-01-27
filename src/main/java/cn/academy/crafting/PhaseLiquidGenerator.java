/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegWorldGen;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.Random;

/**
 * @author WeAthFolD
 */
@Registrant
public class PhaseLiquidGenerator implements IWorldGenerator {

    @RegWorldGen(1)
    public static PhaseLiquidGenerator INSTANCE;

    WorldGenPhaseLiq genLakes = new WorldGenPhaseLiq();

    public PhaseLiquidGenerator() {
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
        if(ModuleCrafting.GENERATE_PHASE_LIQUID) {
            switch (world.provider.dimensionId) {
            case 0:
                if (random.nextDouble() < 0.3)
                    genLakes.generate(world, random, chunkX * 16 + random.nextInt(16), 5 + random.nextInt(30),
                            chunkZ * 16 + random.nextInt(16));
                break;
            }
        }
    }

}
