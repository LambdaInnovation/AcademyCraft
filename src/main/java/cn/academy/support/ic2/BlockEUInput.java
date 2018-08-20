package cn.academy.support.ic2;

import cn.academy.support.BlockConverterBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * 
 * @author KSkun
 */
public class BlockEUInput extends BlockConverterBase {

    public BlockEUInput() {
        super( "EU", "IF", TileEUInput.class);
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new TileEUInput();
    }

}