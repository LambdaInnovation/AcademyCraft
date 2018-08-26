package cn.academy.support.rf;

import cn.academy.support.BlockConverterBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockRFOutput extends BlockConverterBase {

    public BlockRFOutput() {
        super( "IF", "RF", TileRFOutput.class);
    }
    
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileRFOutput();
    }
    
}