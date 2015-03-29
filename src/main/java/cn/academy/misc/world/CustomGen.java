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
package cn.academy.misc.world;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.feature.WorldGenerator;

/**
 * @author WeathFolD
 *
 */
public class CustomGen {

    private WorldGenerator gen;
    
    int yLimit;
    int densityPerChunk;
    int[] biomeIds;
    
    public CustomGen(WorldGenerator _gen, int _yLimit, int _density, int... _biomeIds) {
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
