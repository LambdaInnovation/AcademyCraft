package cn.academy.energy.internal;

import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.api.IWirelessGenerator;
import cn.academy.energy.api.IWirelessMatrix;
import cn.academy.energy.api.IWirelessNode;
import cn.academy.energy.api.IWirelessReceiver;
import cn.academy.energy.api.IWirelessTile;

public enum BlockType {
    MATRIX(IWirelessMatrix.class), NODE(IWirelessNode.class), 
    RECEIVER(IWirelessReceiver.class), GENERATOR(IWirelessGenerator.class);
    
    private final Class<? extends IWirelessTile> clazz;
    BlockType(Class<? extends IWirelessTile> _clazz) {
        clazz = _clazz;
    }
    
    public boolean validate(TileEntity te) {
        return clazz.isInstance(te);
    }
}
