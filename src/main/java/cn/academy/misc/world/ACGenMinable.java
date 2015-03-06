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

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.feature.WorldGenMinable;

/**
 * A WorldGen for mineral generating.
 * @author KSkun
 */
public class ACGenMinable extends WorldGenMinable {
	
	/** y coordinate */
	private int y;
	/** The number of mineral vein */
	private int vnumber;
	/** The array of mineral generating biome IDs */
	private int[] arrBiomeId = new int[] {};

    /**
     * A WorldGen for mineral generating.
     * Use Normal target (Stone).
     * @param block Mineral Block Instance
     * @param bnumber The number of block
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     */
    public ACGenMinable(Block block, int bnumber, int y, int vnumber) {
        this(block, bnumber, Blocks.stone, y, vnumber);
    }
    
    /**
     * A WorldGen for mineral generating.
     * Use Normal target (Stone).
     * @param block Mineral Block Instance
     * @param bnumber The number of block
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     */
    public ACGenMinable(Block block, int meta, int bnumber, int y, int vnumber) {
        this(block, meta, bnumber, Blocks.stone, y, vnumber);
    }
    
    /**
     * A WorldGen for mineral generating.
     * Use Normal target (Stone).
     * @param block Mineral Block Instance
     * @param bnumber The number of block
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     * @param biomeId The array of generating biome ID
     */
    public ACGenMinable(Block block, int bnumber, int y, int vnumber, int[] biomeId) {
        this(block, bnumber, Blocks.stone, y, vnumber, biomeId);
    }
    
    /**
     * A WorldGen for mineral generating.
     * Use Normal target (Stone).
     * @param block Mineral Block Instance
     * @param meta Block metadata
     * @param bnumber The number of block
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     * @param biomeId The array of generating biome ID
     */
    public ACGenMinable(Block block, int meta, int bnumber, int y, int vnumber, int[] biomeId) {
        this(block, meta, bnumber, Blocks.stone, y, vnumber, biomeId);
    }

    /**
     * A WorldGen for mineral generating.
     * @param block Mineral Block Instance
     * @param bnumber The number of block
     * @param targer Generating target
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     */
    public ACGenMinable(Block block, int bnumber, Block targer, int y, int vnumber) {
    	super(block, bnumber, targer);
    	this.y = y;
    	this.vnumber = vnumber;
    }

    /**
     * A WorldGen for mineral generating with block metadata.
     * @param block Mineral Block Instance
     * @param meta Block metadata
     * @param bnumber The number of block
     * @param targer Generating target
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     */
    public ACGenMinable(Block block, int meta, int bnumber, Block target, int y, int vnumber) {
        super(block, meta, bnumber, target);
    	this.y = y;
    	this.vnumber = vnumber;
    }
    
    /**
     * A WorldGen for mineral generating.
     * @param block Mineral Block Instance
     * @param bnumber The number of block
     * @param targer Generating target
     * @param y y coordinate limit
     * @param vnumber The number of mineral vein
     * @param biomeId The array of generating biome ID
     */
    public ACGenMinable(Block block, int bnumber, Block targer, int y, int vnumber, int[] biomeId) {
    	super(block, bnumber, targer);
    	this.y = y;
    	this.vnumber = vnumber;
    	this.arrBiomeId = biomeId;
    }

    public ACGenMinable(Block block, int meta, int bnumber, Block target, int y, int vnumber, int[] biomeId) {
        super(block, meta, bnumber, target);
    	this.y = y;
    	this.vnumber = vnumber;
    	this.arrBiomeId = biomeId;
    }
    
    public boolean generate(World world, Random rand, int x, int z) {
    	if(arrBiomeId.length == 0) {
        	for(int i = 1; i < vnumber; ++i) {
        		int xcoord = x + rand.nextInt(16);
        		int ycoord = rand.nextInt(y);
        		int zcoord = z + rand.nextInt(16);
        		super.generate(world, rand, xcoord, ycoord, zcoord);
        	}
    	} else {
    		WorldChunkManager chunkmgr = new WorldChunkManager(world);
    		int biomeId = chunkmgr.getBiomeGenAt(x, z).biomeID;
    		for(int i : arrBiomeId) {
    			if(biomeId == i) {
    	        	for(int j = 1; j < vnumber; ++j) {
    	        		int xcoord = x + rand.nextInt(16);
    	        		int ycoord = rand.nextInt(y);
    	        		int zcoord = z + rand.nextInt(16);
    	        		super.generate(world, rand, xcoord, ycoord, zcoord);
    	        	}
    			}
    		}
    	}
    	return true;
    }

}
