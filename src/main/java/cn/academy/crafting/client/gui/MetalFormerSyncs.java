package cn.academy.crafting.client.gui;

import cn.academy.crafting.block.TileMetalFormer;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class MetalFormerSyncs {
	
	@RegNetworkCall(side = Side.SERVER)
	public static void cycle(@Instance TileMetalFormer former) {
		former.cycleMode();
	}
	
}
