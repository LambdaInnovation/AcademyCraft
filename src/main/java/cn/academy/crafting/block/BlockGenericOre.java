package cn.academy.crafting.block;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.ACBlock;
import cn.lambdalib2.util.generic.RandUtils;
import net.minecraft.block.BlockOre;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class BlockGenericOre extends BlockOre {
    
    int rangeFrom = 1, rangeTo = 2;
    Item itemDropped;

    public BlockGenericOre(String name, float hardness, int harvestLevel) {
        setHardness(hardness);
        setHarvestLevel("pickaxe", harvestLevel);
        setCreativeTab(AcademyCraft.cct);
        setBlockTextureName("academy:" + name);
        setBlockName("ac_" + name);
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