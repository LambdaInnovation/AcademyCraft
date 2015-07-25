/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.client.gui;

import net.minecraft.tileentity.TileEntity;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.internal.NodeConn;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@Registrant
public class LinkToNodeSyncs {
	
	@RegNetworkCall(side = Side.SERVER)
	public static void retrieveNearbyNetworks(@Instance TileEntity te, @Instance Future future) {
		IWirelessUser user = (IWirelessUser) te;
		future.setAndSync(WirelessHelper.getNodesInRange(te.getWorldObj(), te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5));
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void retrieveCurrentLink(@Instance TileEntity te, @Data Future future) {
		IWirelessUser user = (IWirelessUser) te;
		NodeConn conn = WirelessHelper.getNodeConn(user);
		
		future.setAndSync(conn == null ? "" : conn.getNode().getNodeName());
	}
	
}
