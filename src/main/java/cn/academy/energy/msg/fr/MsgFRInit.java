/**
 * 
 */
package cn.academy.energy.msg.fr;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import cn.academy.api.energy.IWirelessNode;
import cn.academy.core.energy.WirelessSystem;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.academy.energy.client.gui.GuiFreqRegulator;
import cn.academy.energy.item.ItemFreqRegulator;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegMessageHandler;
import cn.liutils.util.misc.Pair;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class MsgFRInit implements IMessage {
	
	Map<String, int[]> cnMap = new HashMap();
	String curChannel;

	public MsgFRInit(TileUserBase tile) {
		curChannel = WirelessSystem.getTileChannel(tile);
		List<Pair<IWirelessNode, String>> nearNodes = tile.getAvailableNodes(ItemFreqRegulator.LIST_MAX);
		for(int i = 0; i < nearNodes.size() && i < ItemFreqRegulator.LIST_MAX; ++i) {
			Pair<IWirelessNode, String> pair = nearNodes.get(i);
			TileEntity te = (TileEntity) pair.first;
			cnMap.put(pair.second, new int[] { te.xCoord, te.yCoord, te.zCoord });
		}
	}
	
	public MsgFRInit() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		curChannel = ByteBufUtils.readUTF8String(buf);
		if(curChannel == "") curChannel = null;
		int n = buf.readInt();
		while(n-- > 0) {
			String str = ByteBufUtils.readUTF8String(buf);
			int[] arr = new int[] { buf.readInt(), buf.readInt(), buf.readInt() };
			cnMap.put(str, arr);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		if(curChannel == null) curChannel = "";
		ByteBufUtils.writeUTF8String(buf, curChannel);
		buf.writeInt(cnMap.size());
		for(Entry<String, int[]> ent : cnMap.entrySet()) {
			ByteBufUtils.writeUTF8String(buf, ent.getKey());
			int[] arr = ent.getValue();
			for(int i = 0; i < 3; ++i) {
				buf.writeInt(arr[i]);
			}
		}
	}
	
	@RegMessageHandler(msg = MsgFRInit.class, side = RegMessageHandler.Side.CLIENT)
	public static class Handler implements IMessageHandler<MsgFRInit, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(MsgFRInit msg, MessageContext ctx) {
			GuiScreen gs = Minecraft.getMinecraft().currentScreen;
			if(!(gs instanceof GuiFreqRegulator))
				return null;
			GuiFreqRegulator fr = (GuiFreqRegulator) gs;
			fr.synced = true;
			fr.channels = msg.cnMap;
			fr.curChannel = msg.curChannel;
			return null;
		}
		
	}

}
