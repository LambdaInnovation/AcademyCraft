/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui;

import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.tileentity.TileEntity;

@Registrant
public class EnergyUISyncs {

    @RegNetworkCall(side = Side.SERVER)
    public static void syncIsLinked(@Instance TileEntity te, @Data Future future) {
        if(te instanceof IWirelessGenerator) {
            future.setAndSync(WirelessHelper.isGeneratorLinked((IWirelessGenerator) te));
        } else if(te instanceof IWirelessReceiver) {
            future.setAndSync(WirelessHelper.isReceiverLinked((IWirelessReceiver) te));
        } else {
            future.setAndSync(false);
        }
    }
    
}
