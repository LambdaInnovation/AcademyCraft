package cn.academy.support.ic2;

import cn.academy.support.BlockConverterBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * 
 * @author KSkun
 */
public class BlockEUOutput extends BlockConverterBase {

    public BlockEUOutput() {
        super( "IF", "EU", TileEUOutput.class);
    }
    
    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileEUOutput();
    }

}