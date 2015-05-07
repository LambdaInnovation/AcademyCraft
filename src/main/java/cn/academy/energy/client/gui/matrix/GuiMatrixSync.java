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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.energy.api.event.ChangePassEvent;
import cn.academy.energy.api.event.CreateNetworkEvent;
import cn.academy.energy.block.TileMatrix;
import cn.academy.energy.internal.WirelessSystem;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@RegistrationClass
public class GuiMatrixSync {
	
	public enum ActionResult {
		WAITING, //Should never send this one.
		INVALID_INPUT,
		SUCCESS,
		FAIL
	}

	public static void sendSyncRequest(GuiMatrix gui) {
		receivedRequest(gui.tile);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void fullInit(@Instance TileMatrix matrix, @Data String ssid, @Data String password) {
		if(MinecraftForge.EVENT_BUS.post(new CreateNetworkEvent(matrix, ssid, password))) {
			result(matrix, ActionResult.FAIL);
		} else {
			result(matrix, ActionResult.SUCCESS);
		}
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void passwordUpdate(@Instance TileMatrix matrix, @Data String oldPass, @Data String newPass) {
		if(MinecraftForge.EVENT_BUS.post(new ChangePassEvent(matrix, oldPass, newPass))) {
			result(matrix, ActionResult.FAIL);
		} else {
			result(matrix, ActionResult.SUCCESS);
		}
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void result(@Instance TileMatrix matrix, @Instance ActionResult result) {
		GuiMatrix gui = locate(matrix);
		if(gui != null)
			gui.receiveActionResult(result, true);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	public static void receivedRequest(@Instance TileMatrix matrix) {
		if(matrix == null)
			return;
		//Extract out the stuffs
		NBTTagCompound tag = new NBTTagCompound();
		boolean loaded = WirelessSystem.instance.isTileActive(matrix);
		tag.setBoolean("loaded", loaded);
		int cap = matrix.getCapacity();
		double lat = matrix.getLatency();
		double range = matrix.getRange();
		tag.setByte("capacity", (byte) cap);
		tag.setInteger("latency", (int) lat);
		tag.setInteger("range", (int) range);
		
		if(loaded) {
			String ssid = WirelessSystem.instance.getSSID(matrix);
			tag.setString("ssid", ssid);
		}
		
		//Throw to client
		receivedReply(matrix, tag);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	public static void receivedReply(@Instance TileMatrix matrix, @Data NBTTagCompound tag) {
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
