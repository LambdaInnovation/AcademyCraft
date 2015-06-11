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
package cn.academy.energy.client.gui.matrix;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.event.wen.ChangePassEvent;
import cn.academy.energy.api.event.wen.CreateNetworkEvent;
import cn.academy.energy.block.TileMatrix;
import cn.academy.energy.internal.WirelessNet;
import cn.academy.energy.internal.WirelessSystem;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class GuiMatrixSync {
	
	public enum ActionResult {
		WAITING("loading"), //Should never send this one.
		INVALID_INPUT("reject"),
		SUCCESS("confirmed"),
		FAIL("reject");
		
		public final ResourceLocation markSrc;
		private final String translateKey;
		
		public String getDescription() {
			return StatCollector.translateToLocal(translateKey);
		}
		
		ActionResult(String logo) {
			markSrc = new ResourceLocation("academy:textures/guis/mark/mark_" + logo + ".png");
			translateKey = "ac.gui.matrix.mark." + this.toString().toLowerCase() + ".desc";
		}
	}

	public static void sendSyncRequest(GuiMatrix gui) {
		receivedRequest(Minecraft.getMinecraft().thePlayer, gui.tile);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void fullInit(@Instance EntityPlayer player, @Instance TileMatrix matrix, @Data String ssid, @Data String password) {
		if(MinecraftForge.EVENT_BUS.post(new CreateNetworkEvent(matrix, ssid, password))) {
			result(player, matrix, ActionResult.FAIL);
		} else {
			result(player, matrix, ActionResult.SUCCESS);
		}
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void passwordUpdate(@Instance EntityPlayer player, @Instance TileMatrix matrix, @Data String oldPass, @Data String newPass) {
		if(MinecraftForge.EVENT_BUS.post(new ChangePassEvent(matrix, oldPass, newPass))) {
			result(player, matrix, ActionResult.FAIL);
		} else {
			result(player, matrix, ActionResult.SUCCESS);
		}
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void result(@Target EntityPlayer player, @Instance TileMatrix matrix, @Instance ActionResult result) {
		GuiMatrix gui = locate(matrix);
		if(gui != null)
			gui.receiveActionResult(result, true);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void receivedRequest(@Instance EntityPlayer player, @Instance TileMatrix matrix) {
		if(matrix == null)
			return;
		//Extract out the stuffs
		NBTTagCompound tag = new NBTTagCompound();
		boolean loaded = WirelessHelper.isMatrixActive(matrix);
		tag.setBoolean("loaded", loaded);
		int cap = matrix.getCapacity();
		double lat = matrix.getLatency();
		double range = matrix.getRange();
		tag.setByte("capacity", (byte) cap);
		tag.setInteger("latency", (int) lat);
		tag.setInteger("range", (int) range);
		tag.setByte("nodes", (byte) 0);
		
		System.out.println("Loaded: " + loaded);
		
		if(loaded) {
			WirelessNet net = WirelessHelper.getWirelessNet(matrix);
			String ssid = net.getSSID();
			
			tag.setString("ssid", ssid);
			tag.setByte("nodes", (byte) net.getLoad());
		}
		
		//Throw to client
		receivedReply(player, matrix, tag);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receivedReply(@Target EntityPlayer player, @Instance TileMatrix matrix, @Data NBTTagCompound tag) {
		GuiMatrix gui = locate(matrix);
		if(gui != null)
			gui.receiveSync(tag);
	}
	
	@SideOnly(Side.CLIENT)
	private static GuiMatrix locate(TileMatrix matrix) {
		GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		if(screen instanceof GuiMatrix) {
			GuiMatrix mat = (GuiMatrix) screen;
			if(mat.tile == matrix) { //Double-check tile instance
				return mat;
			}
		}
		return null;
	}
	
}
