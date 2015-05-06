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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import cn.academy.energy.block.ContainerMatrix;
import cn.academy.energy.block.TileMatrix;
import cn.academy.energy.client.gui.matrix.GuiMatrixSync.ActionResult;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.cgui.utils.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * TODO: Localization
 * @author WeAthFolD
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
@RegSubmoduleInit
public class GuiMatrix extends LIGuiContainer {
	
	static LIGui loaded;
	
	public static void init() {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/matrix.xml"));
	}
	
	//--------
	
	//Synced states
	boolean receivedSync;
	
	boolean isLoaded;
	
	String ssid;
	int nodes;
	int capacity;
	int latency;
	int range;
	
	//Action
	boolean waitingForResult;
	long resultReceivedTime; //Used for anim display time ctrl.
	ActionResult result;
	
	//Meta objects
	final TileMatrix tile;
	
	final ContainerMatrix container;
	
	Widget pageMain, pageSSID;

	public GuiMatrix(ContainerMatrix c) {
		super(c);
		tile = c.tile;
		container = c;
		
		load();
	}
	
	public void receiveSync(NBTTagCompound tag) {
		if(!receivedSync) {
			receivedSync = true;
			
			isLoaded = tag.getBoolean("loaded");
			capacity = tag.getInteger("capacity");
			latency = tag.getInteger("latency");
			range = tag.getInteger("range");
			
			//Setup the info about matrix itself
			TextBox box;
			box = pageMain.getWidget("info_Node2").getComponent("TextBox");
			box.content = nodes + "/" + capacity;
			
			box = pageMain.getWidget("info_BM2").getComponent("TextBox");
			box.content = String.format("%d", latency);
			
			box = pageMain.getWidget("info_Range2").getComponent("TextBox");
			box.content = String.format("%d", range);
			
			box = pageMain.getWidget("info_ssid2").getComponent("TextBox");
			if(isLoaded) {
				ssid = tag.getString("ssid");
				box.content = ssid;
			} else {
				box.content = "Not Loaded";
			}
		}
	}
	
	/**
	 * May called by sync method or gui itself, to update state animation.
	 */
	public void receiveActionResult(ActionResult result) {
		if(waitingForResult) {
			waitingForResult = false;
			this.result = result;
			resultReceivedTime = Minecraft.getSystemTime();
		}
	}
	
	private void load() {
		GuiMatrixSync.sendSyncRequest(this);
		
		LIGui gui = getGui();
		pageMain = loaded.getWidget("Main").copy();
		pageSSID = loaded.getWidget("SSIDINIT").copy();
		
		gui.addWidget(pageMain);
		gui.addWidget(pageSSID);
		
		pageSSID.transform.doesDraw = false;
		
		wrapButton(pageMain.getWidget("button"));
		
		wrapButton(pageSSID.getWidget("button_YES"));
		wrapButton(pageSSID.getWidget("button_NO"));
		
		EventLoader.load(pageMain, new MainCallback());
		EventLoader.load(pageSSID, new SSIDCallback());
	}
	
	@Override
    public boolean isSlotActive() {
    	return !pageSSID.transform.doesDraw;
    }
	
	private void wrapButton(Widget w) {
		DrawTexture drawer = w.getComponent("DrawTexture");
		final Color hoverColor = new Color(1, 1, 1, 1), idleColor = new Color(1, 1, 1, 0.3);
		drawer.color = idleColor;
		
		w.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture drawer = w.getComponent("DrawTexture");
				drawer.color = event.hovering ? hoverColor : idleColor;
			}
		});
	}
	
	private void onDialogueOpen() {
		TextBox box = pageSSID.getWidget("Text_SSID").getComponent("TextBox");
		
		if(isLoaded) {
			box.content = ssid;
			box.allowEdit = false;
		} else {
			box.allowEdit = true;
		}
		
		box = pageSSID.getWidget("Text_Pass").getComponent("TextBox");
		box.content = "";
		
		box = pageSSID.getWidget("Text_Confirm").getComponent("TextBox");
		box.content = "";
	}
	
	public class MainCallback {
		
		@GuiCallback("button")
		public void openDialogue(Widget w, MouseDownEvent event) {
			pageSSID.transform.doesDraw = true;
			onDialogueOpen();
		}
		
		@GuiCallback
		public void blackout(Widget w, FrameEvent event) {
			if(pageSSID.transform.doesDraw) {
				LIGui.drawBlackout();
			}
		}
		
	}

	public class SSIDCallback {
		
		@GuiCallback("button_YES")
		public void yesDown(Widget w, MouseDownEvent event) {
			String pwd = getPassword();
			
			resultReceivedTime = Minecraft.getSystemTime();
			waitingForResult = true;
			if(pwd == null) {
				//Quit with error
				receiveActionResult(ActionResult.INCPASS);
			} else if(isLoaded) {
				//Update password
				GuiMatrixSync.passwordUpdate(tile, "", "");//TODO
			} else {
				//Full init
				GuiMatrixSync.fullInit(tile, getSSID(), pwd);
			}
			pageSSID.transform.doesDraw = false;
		}
		
		@GuiCallback("button_NO")
		public void noDown(Widget w, MouseDownEvent event) {
			//Close without doing anything
			pageSSID.transform.doesDraw = false;
		}
		
		private String getSSID() {
			TextBox box = pageSSID.getWidget("Text_SSID").getComponent("TextBox");
			return box.content;
		}
		
		private String getPassword() {
			TextBox b1 = pageSSID.getWidget("Text_Pass").getComponent("TextBox"), b2 = pageSSID.getWidget("Text_Confirm").getComponent("TextBox");
			return b1.content.equals(b2.content) ? b1.content : null;
		}
		
	}
	
}
