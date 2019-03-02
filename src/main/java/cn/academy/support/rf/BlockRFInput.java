package cn.academy.support.rf;

import cn.academy.support.BlockConverterBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockRFInput extends BlockConverterBase {
    
    public BlockRFInput() {
        super( "RF", "IF", TileRFInput.class);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRFInput();
    }
    
}