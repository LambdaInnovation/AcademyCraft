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

import net.minecraft.util.ResourceLocation;
import cn.academy.energy.block.ContainerMatrix;
import cn.academy.energy.block.TileMatrix;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
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
	
	final TileMatrix tile;
	
	final ContainerMatrix container;
	
	Widget pageMain, pageSSID;

	public GuiMatrix(ContainerMatrix c) {
		super(c);
		tile = c.tile;
		container = c;
		
		load();
	}
	
	private void load() {
		LIGui gui = getGui();
		pageMain = loaded.getWidget("Main").copy();
		pageSSID = loaded.getWidget("SSIDINIT").copy();
		
		gui.addWidget(pageMain);
		gui.addWidget(pageSSID);
		
		pageSSID.transform.doesDraw = false;
		
		handleButton(pageMain.getWidget("button"));
		
		handleButton(pageSSID.getWidget("button_YES"));
		handleButton(pageSSID.getWidget("button_NO"));
		
		EventLoader.load(pageMain, new MainCallback());
		EventLoader.load(pageSSID, new SSIDCallback());
	}
	
	@Override
    public boolean isSlotActive() {
    	return !pageSSID.transform.doesDraw;
    }
	
	private void handleButton(Widget w) {
		DrawTexture drawer = w.getComponent("DrawTexture");
		drawer.enabled = false;
		
		w.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture drawer = w.getComponent("DrawTexture");
				drawer.enabled = event.hovering;
			}
		});
	}
	
	public class MainCallback {
		
		@GuiCallback("button")
		public void openDialogue(Widget w, MouseDownEvent event) {
			pageSSID.transform.doesDraw = true;
		}
		
	}

	public class SSIDCallback {
		
		@GuiCallback
		public void blackout(Widget w, FrameEvent event) {
			LIGui.drawBlackout();
		}
		
		@GuiCallback("button_YES")
		public void yesDown(Widget w, MouseDownEvent event) {
			
			pageSSID.transform.doesDraw = false;
		}
		
		@GuiCallback("button_NO")
		public void noDown(Widget w, MouseDownEvent event) {
			
			pageSSID.transform.doesDraw = false;
		}
		
	}
	
}
