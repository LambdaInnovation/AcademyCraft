package cn.academy.block.block;

import cn.academy.AcademyCraft;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.block.BlockOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import java.util.Random;

/**
 * @author WeAthFolD
 */
public class BlockGenericOre extends BlockOre {
    
    private int rangeFrom = 1, rangeTo = 2;
    private Item itemDropped;

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
    public Item getItemDropped(IBlockState blockState, Random b, int c) {
        if(itemDropped == null)
            itemDropped = Item.getItemFromBlock(this);
        return itemDropped;
    }

}