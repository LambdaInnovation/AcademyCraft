package cn.academy.worldgen;

import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class PhaseLiquidGenerator implements IWorldGenerator {

    //@RegWorldGen(1)
    @StateEventCallback
    public static void preInit(FMLPreInitializationEvent event)
    {
        GameRegistry.registerWorldGenerator(INSTANCE,1);
    }
    public static final PhaseLiquidGenerator INSTANCE = new PhaseLiquidGenerator();

    WorldGenPhaseLiq genLakes = new WorldGenPhaseLiq();

    public PhaseLiquidGenerator() {}

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if(WorldGenInit.GENERATE_PHASE_LIQUID &&
                world.provider.getDimensionType()== DimensionType.OVERWORLD) {
            if (random.nextDouble() < 0.3) {
                genLakes.generate(world, random,
                    new BlockPos(chunkX * 16 + random.nextInt(16)+8, 5 + random.nextInt(30), chunkZ * 16 + random.nextInt(16)+8)
                );
            }
        }
    }

}