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
import java.util.List;

import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.xml.CGUIDocument;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cn.academy.core.AcademyCraft;
import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.academy.energy.client.gui.node.GuiNodeSync.CheckState;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ElementList;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.VerticalDragBar;
import cn.lambdalib.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.Color;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeathFolD
 *
 */
@Registrant
public class GuiNode extends CGuiScreenContainer {
	
	public static final WidgetContainer document = CGUIDocument.panicRead(new ResourceLocation("academy:guis/node.xml"));
	
	final ContainerNode container;
	final TileNode tile;
	
	Widget pageMain, pageSelect;
	
	MainHandler mainHandler;
	SelectHandler selectHandler;
	
	// States
	CheckState state;
	private boolean infoSynced, listSynced;
	
	// Sync info
	private int load;
	private int maxLoad;
	private String nodeName = "loading...";
	private String ssid;
	
	// Sync list info
	private List<String> networks = new ArrayList<>();
    
    public GuiNode(ContainerNode c) {
        super(c);
        container = c;
        tile = c.node;
        
        create();
        
        GuiNodeSync.doQueryInfo(tile);
        GuiNodeSync.doQueryList(tile);
    }
    
    @Override
    protected boolean containerAcceptsKey(int key) {
    	Widget focus = gui.getFocus();
    	boolean isinput = focus != null && focus.getName().equals("input_name");
    	return key == Keyboard.KEY_ESCAPE || !isinput;
    }
    
	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y) {
		GL11.glPushMatrix();
		GL11.glTranslated(-guiLeft, -guiTop, 0);
		
		Widget w = gui.getTopWidget(x, y);
		if(w != null) {
			String text = null;
			switch(w.getName()) {
			case "progress_load":
				text = load + "/" + maxLoad;
				break;
			case "progress_imag":
				text = String.format("%.1f/%.1fIF", tile.getEnergy(), tile.getMaxEnergy());
				break;
			case "btn_edit":
				text = local("rename");
				break;
			case "button_confirm":
				text = StatCollector.translateToLocal("ac.network.link");
				break;
			case "button_config":
				text = StatCollector.translateToLocal("ac.network.search");
				break;
			}
			
			if(text != null) {
				EnergyUIHelper.drawTextBox(text, x + 5, y, 9);
			}
		}
		
		GL11.glPopMatrix();
	}
    
    public static String local(String name) {
    	return StatCollector.translateToLocal("ac.gui.node." + name + ".desc");
    }
    
    public void receivedInitSync(int load, String nodeName, String ssid) {
    	if(!infoSynced) {
    		infoSynced =  true;
    		
	    	this.load = load;
	    	this.nodeName = nodeName;
	    	this.ssid = ssid;
	    	
	    	mainHandler.receivedInit();
    	}
    }
    
    public void receivedListSync(List<String> ret) {
    	if(!listSynced) {
    		listSynced = true;
    		networks = ret;
    	}
    }
    
    public void receivedActionState(CheckState state) {
    	mainHandler.changeState(state);
    }
    
    private void create() {
    	//Basic init
    	pageMain = document.getWidget("window_main").copy();
    	pageSelect = document.getWidget("window_ssidselect").copy();
    	
    	gui.addWidget(pageMain);
    	gui.addWidget(pageSelect);
    	
    	pageSelect.transform.doesDraw = false;
    	
    	state = CheckState.LOADING;
    	maxLoad = tile.getCapacity();
    	
    	pageSelect.listen(FrameEvent.class, (w, e) ->
    	{
    		RenderUtils.drawBlackout();
    	});
    	
    	//Callback
		mainHandler = new MainHandler();
		selectHandler = new SelectHandler();
    }
    
    public static void wrapButton(Widget button, double alpha0) {
    	wrapButton(button, alpha0, 1);
    }
    
    public static void wrapButton(Widget button, double alpha0, double alpha1) {
    	final DrawTexture drawer = DrawTexture.get(button);
    	final Color idle = new Color(1, 1, 1, alpha0), active = new Color(1, 1, 1, alpha1);
    	drawer.color = idle;
    	
    	button.listen(FrameEvent.class, (w, event) ->
    	{
    		drawer.color = event.hovering ? active : idle;
    	});
    }
    
    private void openSelect() {
    	pageMain.transform.doesListenKey = false;
    	pageSelect.transform.doesDraw = true;
    	selectHandler.init();
    }
    
    private void closeSelect() {
    	pageMain.transform.doesListenKey = true;
    	pageSelect.transform.doesDraw = false;
    	selectHandler.cleanup();
    }
    
	@Override
    public boolean isSlotActive() {
    	return pageMain.transform.doesListenKey;
    }
    
    public class MainHandler {
    	
    	public MainHandler() {
    		wrapButton(getWidget("button_config"), 0.6);
    		wrapButton(getWidget("button_confirm"), 0.6);
    		
    		changeState(CheckState.LOADING);

			// Init events
			pageMain.listen(FrameEvent.class, (w, e) -> {
				ProgressBar bar = ProgressBar.get(getWidget("progress_imag"));
				bar.progress = bar.progressDisplay = tile.getEnergy() / tile.getMaxEnergy();
			});

			pageMain.getWidget("button_confirm").listen(LeftClickEvent.class, (w, e) ->
					startLink(TextBox.get(getWidget("input_ssid")).content,
							TextBox.get(getWidget("input_pw")).content)
			);

			pageMain.getWidget("button_config").listen(LeftClickEvent.class, (w, e) -> {
				if(infoSynced && listSynced) {
					openSelect();
				}
			});

			pageMain.getWidget("btn_edit").listen(LeftClickEvent.class, (w, e) -> {
				GuiNodeSync.doRename(GuiNode.this, TextBox.get(getWidget("input_name")).content);
			});
    	}
    	
    	public void changeState(CheckState nstate) {
    		state = nstate;
    		DrawTexture.get(getWidget("mark_state2")).texture = nstate.texture;
    		TextBox.get(getWidget("text_state")).content = nstate.getDisplayName();
    		AcademyCraft.log.info("ChangeState " + state);
    	}
    	
    	public void receivedInit() {
    		changeState(CheckState.IDLE);
    		
    		ProgressBar bar = ProgressBar.get(getWidget("progress_load"));
    		bar.progress = (double)load / maxLoad;
    		
    		TextBox.get(getWidget("input_name")).content = nodeName;
    		
    		updateSSID(ssid);
    	}
    	
    	public void updateSSID(String s) {
    		TextBox.get(getWidget("input_ssid")).content = s;
    		TextBox.get(getWidget("input_pw")).content = "";
    	}
    	
    	private void startLink(String ssid, String pass) {
    		changeState(CheckState.TRANSMITTING);
    		GuiNodeSync.doLogin(GuiNode.this, ssid, pass);
    	}
    	
    	private Widget getWidget(String name) {
    		return pageMain.getWidget(name);
    	}
    }
    
    public class SelectHandler {
    	
    	public SelectHandler() {
    		final Widget slide = getWidget("button_slide");
    		wrapButton(slide, 0.5);
    		//slide.addComponent(new VerticalDragBar().setArea(*, _y1));
    		slide.listen(DraggedEvent.class, (w, e) -> 
    		{
    			VerticalDragBar db = VerticalDragBar.get(w);
				ElementList elist = ElementList.get(getWidget("list"));
				elist.setProgress((int) Math.round((db.getProgress() * elist.getMaxProgress())));
    		});
    		
    		wrapButton(getWidget("button_close"), 0.6);

			pageSelect.getWidget("button_close").listen(LeftClickEvent.class, (w, e) -> closeSelect());
			pageSelect.getWidget("btn_disconnect").listen(LeftClickEvent.class, (w, e) -> {
				ssid = "";
				GuiNodeSync.doDisconnect(tile);
				updateCurrentNet();
			});
    	}
    	
    	/**
    	 * Called each time the select page is opened.
    	 */
    	public void init() {
    		updateCurrentNet();
    		
    		Widget slide = getWidget("button_slide");
    		VerticalDragBar.get(slide).setProgress(0);
    		
    		Widget list = getWidget("list");
    		ElementList eList = new ElementList();
    		eList.spacing = 4.0;
    		
    		for(String s : networks) {
    			Widget single = getWidget("ssid_template").copy();
    			single.addComponent(new DrawTexture().setTex(null));
    			
    			TextBox.get(single).content = s;
    			
    			single.listen(LeftClickEvent.class, (w, e) -> 
    			{
    				mainHandler.updateSSID(TextBox.get(w).content);
					closeSelect();
    			});
    			
    			wrapButton(single, 0.0, 0.3);
    			
    			eList.addWidget(single);
    		}
    		list.addComponent(eList);
    	}
    	
    	/**
    	 * Called each time the page is closed.
    	 */
    	public void cleanup() {
    		getWidget("list").removeComponent("ElementList");
    	}
    	
    	private void updateCurrentNet() {
    		TextBox.get(getWidget("text_currentnet2")).content = ssid.equals("") ? local("not_connected") : ssid;
    	}
    	
    	private Widget getWidget(String name) {
    		return pageSelect.getWidget(name);
    	}
    	
    }
    
}
