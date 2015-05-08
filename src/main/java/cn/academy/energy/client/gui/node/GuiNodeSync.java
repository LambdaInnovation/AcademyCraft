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
import net.minecraftforge.common.MinecraftForge;
import cn.academy.energy.api.event.LinkNodeEvent;
import cn.academy.energy.block.TileNode;
import cn.academy.energy.internal.WirelessSystem;
import cn.annoreg.core.RegistrationClass;
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
@RegistrationClass
public class GuiNodeSync {
	
	enum CheckState { 
		INPUT, TRANSMITTING, SUCCESSFUL, FAILED;
		public final ResourceLocation texture;
		CheckState() {
			String n = this.toString().toLowerCase();
			texture = new ResourceLocation("academy:textures/guis/mark" + n + ".png");
		}
	};
	
    static class Mat {
    	final String ssid;
    	
    	public Mat(String _ssid) {
    		ssid = _ssid;
    	}
    }
	
	//Init
	public static void doQueryInfo(TileNode target) {
		queryInfo(Minecraft.getMinecraft().thePlayer, target);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void queryInfo(@Instance EntityPlayer player, @Instance TileNode target) {
		if(target == null)
			return;
		receiveInfo(player, target, WirelessSystem.getNodeConnection(target).getLoad(), target.getNodeName());
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receiveInfo(
			@Target EntityPlayer player, 
			@Instance TileNode target, 
			@Data Integer load, 
			@Data String name) {
		GuiNode gui = locate(target);
		if(gui != null) {
			gui.receivedInitSync(load, name);
		}
	}
	
	//List
	public static void doQueryList(TileNode target) {
		//Waiting for finish of array list serializer.
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void queryList(@Instance EntityPlayer player, @Instance TileNode target) {
		if(target == null)
			return;
		Collection<String> ret = WirelessSystem.getAvailableSSIDs(target);
		List<String> list = new ArrayList(ret);
		receiveList(player, target, list);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receiveList(@Target EntityPlayer player, @Instance TileNode target, @Data List<String> ssids) {
		
	}
	
	//Rename
	
	//Login
	@SideOnly(Side.CLIENT)
	static void tryLogin(GuiNode gui, Mat m, String password) {
		doLogin(Minecraft.getMinecraft().thePlayer, gui.tile, m.ssid, password);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void doLogin(
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
