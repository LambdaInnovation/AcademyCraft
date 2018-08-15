package cn.academy.block.block;

import cn.academy.AcademyCraft;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.block.BlockOre;
import net.minecraft.item.Item;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class BlockGenericOre extends BlockOre {
    
    int rangeFrom = 1, rangeTo = 2;
    Item itemDropped;

    public BlockGenericOre(float hardness, int harvestLevel) {
        setHardness(hardness);
        setHarvestLevel("pickaxe", harvestLevel);
        setCreativeTab(AcademyCraft.cct);
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