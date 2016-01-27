/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting.block;

import cn.academy.core.block.ACBlock;
import cn.lambdalib.util.generic.RandUtils;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class BlockGenericOre extends ACBlock {
    
    int rangeFrom = 1, rangeTo = 2;
    Item itemDropped;

    public BlockGenericOre(String name, float hardness, int harvestLevel) {
        super(name, Material.rock);
        this.setHardness(hardness);
        this.setHarvestLevel("pickaxe", harvestLevel);
    }
    
    public BlockGenericOre setDropData(Item item) {
        itemDropped = item;
        
        return this;
    }
    
    public BlockGenericOre setDropData(Item item, int from, int to) {
        itemDropped = item;
        rangeFrom = from;
        rangeTo = to;
        
        return this;
    }
    
    @Override
    public int quantityDropped(Random r) {
        return RandUtils.rangei(rangeFrom, rangeTo);
    }
    
    @Override
    public Item getItemDropped(int a, Random b, int c) {
        if(itemDropped == null)
            itemDropped = Item.getItemFromBlock(this);
        return itemDropped;
    }

}
