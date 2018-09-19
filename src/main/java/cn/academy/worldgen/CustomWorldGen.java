package cn.academy.worldgen;

import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/**
 * @author WeathFolD
 *
 */
public class CustomWorldGen {

    private WorldGenerator gen;
    
    int yLimit;
    int densityPerChunk;
    int[] biomeIds;
    
    public CustomWorldGen(WorldGenerator _gen, int _yLimit, int _density, int... _biomeIds) {
        gen = _gen;
        yLimit = _yLimit;
        densityPerChunk = _density;
        biomeIds = _biomeIds;
    }
    
    public void generate(World world, Random rand, int chunkMinX, int chunkMinZ) {
        if(!canGen(world, chunkMinX, chunkMinZ))
            return;
        for(int i = 0; i < densityPerChunk; ++i) {
            int x = chunkMinX + rand.nextInt(16),
                y = rand.nextInt(yLimit),
                z = chunkMinZ + rand.nextInt(16);
            
            gen.generate(world, rand, x, y, z);
        }
    }
    
    private boolean canGen(World world, int x, int z) {
        if(biomeIds.length == 0) return true;
        WorldChunkManager chunkmgr = new WorldChunkManager(world);
        int biomeId = chunkmgr.getBiomeGenAt(x, z).biomeID;
        for(int i : biomeIds)
            if(i == biomeId) return true;
        return false;
    }

}