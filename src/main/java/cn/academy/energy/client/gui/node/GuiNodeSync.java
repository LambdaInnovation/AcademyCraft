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
package cn.academy.energy.client.gui.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.event.wen.LinkNodeEvent;
import cn.academy.energy.block.TileNode;
import cn.academy.energy.internal.WirelessNet;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@Registrant
public class GuiNodeSync {
	
	public enum CheckState { 
		LOADING("loading"),
		IDLE("idle"),
		TRANSMITTING("loading", "transmitting"), 
		SUCCESSFUL("confirmed"),
		FAILED("reject");
		
		public final ResourceLocation texture;
		final String translateKey;
		
		String getDisplayName() {
			return StatCollector.translateToLocal(translateKey);
		}
		
		CheckState(String logo, String desc) {
			translateKey = "ac.gui.node.mark." + desc + ".desc";
			texture = new ResourceLocation("academy:textures/guis/mark/mark_" + logo + ".png");
		}
		
		CheckState(String logo) {
			translateKey = "ac.gui.node.mark." + this.toString().toLowerCase() + ".desc";
			texture = new ResourceLocation("academy:textures/guis/mark/mark_" + logo + ".png");
		}
	};
	
	//Init
	public static void doQueryInfo(TileNode target) {
		queryInfo(Minecraft.getMinecraft().thePlayer, target);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void queryInfo(@Instance EntityPlayer player, @Instance TileNode target) {
		if(target == null)
			return;
		WirelessNet net = WirelessHelper.getWirelessNet(target);
		receiveInfo(player, target, WirelessHelper.getNodeConn(target).getLoad(), target.getNodeName(), net == null ? "" : net.getSSID());
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receiveInfo(
			@Target EntityPlayer player, 
			@Instance TileNode target, 
			@Data Integer load, 
			@Data String name,
			@Data String ssid) {
		GuiNode gui = locate(target);
		if(gui != null) {
			gui.receivedInitSync(load, name, ssid);
		}
	}
	
	//List
	public static void doQueryList(TileNode target) {
		queryList(Minecraft.getMinecraft().thePlayer, target);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void queryList(@Instance EntityPlayer player, @Instance TileNode target) {
		if(target == null)
			return;
		Collection<WirelessNet> nets = WirelessHelper.getNetInRange(
			target.getWorldObj(), 
			target.xCoord + 0.5, target.yCoord + 0.5, target.zCoord + 0.5, 
			10, 20);
		List<String> list = new ArrayList();
		for(WirelessNet net : nets) {
			list.add(net.getSSID());
		}
		receiveList(player, target, list);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receiveList(@Target EntityPlayer player, @Instance TileNode target, @Data List<String> ssids) {
		GuiNode gui = locate(target);
		if(gui != null) {
			gui.receivedListSync(ssids);
		}
	}
	
	//Rename
	@SideOnly(Side.CLIENT)
	static void doRename(GuiNode gui, String newName) {
		rename(gui.tile, newName);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void rename(@Instance TileNode tile, @Data String newName) {
		if(tile != null)
			tile.setNodeName(newName);
	}
	
	//Login
	@SideOnly(Side.CLIENT)
	static void doLogin(GuiNode gui, String ssid, String password) {
		login(Minecraft.getMinecraft().thePlayer, gui.tile, ssid, password);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void login(
			@Instance EntityPlayer player, 
			@Instance TileNode target, 
			@Data String ssid, 
			@Data String password) {
		CheckState result;
		if(target == null) {
			result = CheckState.FAILED;
		} else {
			if(MinecraftForge.EVENT_BUS.post(new LinkNodeEvent(target, ssid, password))) {
				result = CheckState.FAILED;
			} else {
				result = CheckState.SUCCESSFUL;
			}
		}
		receiveResult(player, target, result);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receiveResult(
			@Target EntityPlayer player, 
			@Instance TileNode target, 
			@Instance CheckState state) {
		GuiNode node = locate(target);
		if(node != null) {
			node.receivedActionState(state);
		}
	}
	
	private static GuiNode locate(TileNode target) {
		GuiScreen scr = Minecraft.getMinecraft().currentScreen;
		if(scr instanceof GuiNode) {
			GuiNode ret = (GuiNode) scr;
			return ret.tile == target ? ret : null;
		}
		return null;
	}
	
}
