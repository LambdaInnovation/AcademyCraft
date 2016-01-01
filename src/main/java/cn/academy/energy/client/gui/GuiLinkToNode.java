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

import java.util.ArrayList;
import java.util.List;

import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.client.gui.node.GuiNode;
import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.VerticalDragBar;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.networkcall.Future;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.helper.Font.Align;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class GuiLinkToNode extends CGuiScreen {
	
	private interface Callback {
		void invoke();
	}
	
	TileEntity tile;
	
	Widget main;
	
	List<IWirelessNode> nodes = new ArrayList<>();
	
	String msg;
	long msgLength;
	long msgStartTime;
	Callback callback;
	
	public GuiLinkToNode(IWirelessUser _user) {
		this(_user, false);
	}
	
	public GuiLinkToNode(IWirelessUser _user, boolean mono) {
		tile = (TileEntity) _user;
		
		initWidgets(mono);
		LinkToNodeSyncs.retrieveNearbyNetworks(tile, Future.create(
			(Object o) -> {
				nodes = (List<IWirelessNode>) o;
				buildList();
			}));
		
		LinkToNodeSyncs.retrieveCurrentLink(tile, Future.<String>create(
				(String o) -> {
					updateSSID(o);
				}
			));
	}
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
	private void showMessage(String _msg, long time, Callback _callback) {
		msg = StatCollector.translateToLocal("ac.gui.link." + _msg);
		msgLength = time;
		msgStartTime = GameTimer.getTime();
		callback = _callback;
	}
	
	@Override
    public void drawScreen(int mx, int my, float w) {
		super.drawScreen(mx, my, w);
		if(msg != null) {
			long deltaTime = GameTimer.getTime() - msgStartTime;
			if(deltaTime > msgLength) {
				msg = null;
				if(callback != null) {
					callback.invoke();
				}
			} else {
				EnergyUIHelper.drawTextBox(msg, width / 2, height / 2 - 3, 8, 120, Align.CENTER);
			}
		}
	}
	
    @Override
    protected void mouseClicked(int mx, int my, int btn) {
    	if(msg == null)
    		super.mouseClicked(mx, my, btn);
    }
    
    private void updateSSID(String o) {
    	if("".equals(o))
			o = GuiNode.local("not_connected");
		TextBox.get(main.getWidget("text_currentnet2")).content = o;
    }
	
	private void initWidgets(boolean mono) {
		main = GuiNode.document.getWidget("window_ssidselect").copy();
		
		///if(mono)
		//	DrawTexture.get(main).setShaderId(ShaderMono.instance().getProgramID());
		
		final Widget slide = main.getWidget("button_slide");
		GuiNode.wrapButton(slide, 0.5);
		
		slide.listen(DraggedEvent.class, (w, e) -> {
			VerticalDragBar db = VerticalDragBar.get(w);
			ElementList elist = ElementList.get(main.getWidget("list"));
			elist.setProgress((int) Math.round((db.getProgress() * elist.getMaxProgress())));
		});
		
		GuiNode.wrapButton(main.getWidget("button_close"), 0.5);
		main.getWidget("button_close").listen(LeftClickEvent.class, (w, e) ->
		{
			Minecraft.getMinecraft().displayGuiScreen(null);
		});
		
		main.getWidget("btn_disconnect").listen(LeftClickEvent.class, (w, e) ->
		{
			updateSSID("");
			LinkToNodeSyncs.disconnect(tile);
		});
		
		gui.addWidget(main);
	}
	
	private void buildList() {
		ElementList eList = new ElementList();
		eList.spacing = 4.0;
		
		for(IWirelessNode node : nodes) {
			Widget single = main.getWidget("ssid_template").copy();
			single.addComponent(new DrawTexture().setTex(null));
			
			TextBox.get(single).content = node.getNodeName();
			single.listen(LeftClickEvent.class, (w, e) -> {
				showMessage("Linking", 10000, null);
				LinkToNodeSyncs.startLink(tile, (TileEntity) node, 
					Future.create((Boolean b) -> {
						showMessage(b ? "successful" : "failed", 2000, GuiLinkToNode.this::closeGui);
					}));
			});
			GuiNode.wrapButton(single, 0.0, 0.3);
			
			eList.addWidget(single);
		}
		
		main.getWidget("list").addComponent(eList);
	}
	
	private void closeGui() {
		if(Minecraft.getMinecraft().currentScreen == this)
			Minecraft.getMinecraft().displayGuiScreen(null);
	}
	
}
