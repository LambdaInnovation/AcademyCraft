package cn.academy.crafting.client.gui;

import cn.academy.crafting.block.TileMetalFormer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class MetalFormerSyncs {
    
    @RegNetworkCall(side = Side.SERVER)
    public static void cycle(@Instance TileMetalFormer former) {
        former.cycleMode();
    }
    
}
