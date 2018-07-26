package cn.academy.crafting;

import cn.lambdalib2.annoreg.mc.RegWorldGen;
import cpw.mods.fml.common.IWorldGenerator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class PhaseLiquidGenerator implements IWorldGenerator {

    @RegWorldGen(1)
    public static PhaseLiquidGenerator INSTANCE;

    WorldGenPhaseLiq genLakes = new WorldGenPhaseLiq();

    public PhaseLiquidGenerator() {}

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
            IChunkProvider chunkProvider) {
        if(ModuleCrafting.GENERATE_PHASE_LIQUID &&
                world.provider.getDimensionName().equals("Overworld")) {
            if (random.nextDouble() < 0.3) {
                genLakes.generate(world, random, chunkX * 16 + random.nextInt(16), 5 + random.nextInt(30),
                        chunkZ * 16 + random.nextInt(16));
            }
        }
    }

}