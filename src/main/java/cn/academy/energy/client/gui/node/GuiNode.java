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

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.academy.energy.client.gui.node.GuiNodeSync.CheckState;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.ElementList;
import cn.liutils.cgui.gui.component.ProgressBar;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.VerticalDragBar;
import cn.liutils.cgui.gui.component.VerticalDragBar.DraggedEvent;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.cgui.utils.Color;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegSubmoduleInit(side = RegSubmoduleInit.Side.CLIENT_ONLY)
public class GuiNode extends LIGuiContainer {
	
	static LIGui loaded;
	
	final ContainerNode container;
	final TileNode tile;
	
	Widget pageMain, pageSelect, pageCheck;
	

	CheckCallback checkCallback;
	
	// States
	private boolean infoSynced, listSynced;
	
	// Sync info
	private int load;
	private String nodeName = "loading...";
	private String ssid;
	
	// Sync list info
	private List<String> networks = new ArrayList();
    
    public GuiNode(ContainerNode c) {
        super(c);
        container = c;
        tile = c.node;
        create();
        
        GuiNodeSync.doQueryInfo(tile);
        
        List<String> dummy = new ArrayList();
        dummy.add("aaa");
        dummy.add("bbb");
        dummy.add("ccc");
        dummy.add("ddd");
        dummy.add("eee");
        dummy.add("fff");
        dummy.add("ggg");
        dummy.add("hhh");
        dummy.add("iii");
        dummy.add("jjj");
        
        receivedListSync(dummy);
    }
    
    @Override
    protected boolean containerAcceptsKey(int key) {
    	return key == Keyboard.KEY_ESCAPE;
    }
    
    private static String local(String name) {
    	return StatCollector.translateToLocal("ac.gui.node." + name + ".desc");
    }
    
    public void receivedInitSync(int load, String nodeName, String ssid) {
    	if(!infoSynced) {
    		infoSynced =  true;
    		
	    	this.load = load;
	    	this.nodeName = nodeName;
	    	this.ssid = ssid;
	    	
	    	TextBox box = TextBox.get(pageMain.getWidget("text_nodename"));
	    	box.content = nodeName;
	    	box.allowEdit = true;
	    	
	    	box = TextBox.get(pageSelect.getWidget("text_currentnet2"));
	    	box.content = ssid == null ? local("not_connected") : ssid;
    	}
    }
    
    public void receivedListSync(List<String> ret) {
    	if(!listSynced) {
    		listSynced = true;
    		networks = ret;
    	}
    }
    
    public void receivedActionState(CheckState state) {
    	if(pageCheck.transform.doesDraw) {
    		checkCallback.changeState(state);
    	}
    }
    
    private void create() {
    	//Basic init
    	pageMain = loaded.getWidget("window_main").copy();
    	pageSelect = loaded.getWidget("window_ssidselect").copy();
    	pageCheck = loaded.getWidget("window_check").copy();
    	
    	gui.addWidget(pageMain);
    	gui.addWidget(pageSelect);
    	gui.addWidget(pageCheck);
    	
    	pageSelect.transform.doesDraw = false;
    	pageCheck.transform.doesDraw = false;
    	
    	//---Main Page
    	ProgressBar.get(pageMain.getWidget("progress_node")).progress = tile.getEnergy() / tile.getMaxEnergy();
    	
    	wrapThunder(pageMain.getWidget("mark_input"), new Condition() {
			@Override
			public boolean shows() {
				return tile.chargingIn;
			}
    	});
    	wrapThunder(pageMain.getWidget("mark_output"), new Condition() {
			@Override
			public boolean shows() {
				return tile.chargingOut;
			}
    	});
    	wrapThunder(pageMain.getWidget("mark_net"), new Condition() {
			@Override
			public boolean shows() {
				return tile.enabled;
			}
    	});
    	
    	{
    		Widget w = pageMain.getWidget("button_ssid");
    		final DrawTexture drawer = DrawTexture.get(w);
    		drawer.enabled = false;
    		w.regEventHandler(new FrameEventHandler() {
				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					drawer.enabled = event.hovering;
				}
    		});
    	}
    	//---
    	
    	//---Select page
    	{
    		Widget w = pageSelect.getWidget("list");
    		ElementList list = new ElementList();
    		list.spacing = 3.0;
    		w.addComponent(list);
    	}
    	
    	wrapButton(pageSelect.getWidget("button_slide"));
    	wrapButton(pageSelect.getWidget("button_close"));
    	//---
    	
    	//---Check page
    	wrapButton(pageCheck.getWidget("button_yes"));
    	wrapButton(pageCheck.getWidget("button_close"));
    	//---
    	
    	//Event callbacks
    	EventLoader.load(pageMain, new MainCallback());
    	EventLoader.load(pageSelect, new SelectCallback());
    	EventLoader.load(pageCheck, checkCallback = new CheckCallback());
    }
    
    private void wrapThunder(Widget thunder, final Condition cond) {
    	final DrawTexture dt = DrawTexture.get(thunder);
    	dt.enabled = false;
    	thunder.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				dt.enabled = cond.shows();
			}
    	});
    }
    
    private static void wrapButton(Widget button) {
    	final DrawTexture drawer = DrawTexture.get(button);
    	final Color idle = new Color(1, 1, 1, .3), active = new Color(1, 1, 1, 1);
    	drawer.color = idle;
    	button.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				drawer.color = event.hovering ? active : idle;
			}
    	});
    }
    
	@Override
    public boolean isSlotActive() {
    	return pageMain.transform.doesListenKey;
    }

    public static void init() {
    	loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/node.xml"));
    }
    
    interface Condition {
    	boolean shows();
    }
    
    public class MainCallback {
    	
        class SSID extends Component {
        	
        	final String ssid;

    		public SSID(String s) {
    			super("SSID");
    			ssid = s;
    			
    			this.addEventHandler(new MouseDownHandler() {
    				@Override
    				public void handleEvent(Widget w, MouseDownEvent event) {
    					pageSelect.transform.doesDraw = false;
    					//Open the check window~
    					
    					checkCallback.open(ssid);
    				}
    			});
    		}
        	
        }
        
        @GuiCallback("progress_node")
        public void updateProgress(Widget w, FrameEvent event) {
        	ProgressBar.get(w).progress = tile.getEnergy() / tile.getMaxEnergy();
        	
        	if(event.hovering) {
        		List<String> list = new ArrayList();
        		list.add(String.format("%.0f/%.0f IF", tile.getEnergy(), tile.getMaxEnergy()));
        		drawHoveringText(list, (int)event.mx, (int)event.my, Minecraft.getMinecraft().fontRenderer);
        	}
        }
    	
    	@GuiCallback
    	public void drawBlackout(Widget w, FrameEvent event) {
    		if(pageSelect.transform.doesDraw || pageCheck.transform.doesDraw)
    			LIGui.drawBlackout();
    	}
    	
    	@GuiCallback("button_ssid")
    	public void openSSIDPage(Widget button, MouseDownEvent event) {
    		if(infoSynced && listSynced) {
    			//Open the SSID window
    			Widget w = pageSelect.getWidget("list");
    			ElementList list = ElementList.get(w);
    			list.disposeAll(); //clear first
    			
    			for(int i = 0; i < networks.size(); ++i) {
    				Widget ssid = pageSelect.getWidget("ssid_template").copy();
    				ssid.addComponent(new SSID(networks.get(i)));
    				TextBox.get(ssid).content = networks.get(i);
    				ssid.transform.doesDraw = true;
    				list.addWidget(ssid);
    			}
    			
    			pageMain.transform.doesListenKey = false;
    			pageSelect.transform.doesDraw = true;
    		}
    	}
    	
    }
    
    public class SelectCallback {
    	
    	Widget widgetList;
    	
    	ElementList elementList;
    	
    	public SelectCallback() {
    		widgetList = pageSelect.getWidget("list");
    		
    		elementList = ElementList.get(widgetList);
    	}
    	
    	@GuiCallback("button_slide")
    	public void dragged(Widget w, DraggedEvent event) {
    		VerticalDragBar vdb = VerticalDragBar.get(w);
    		elementList.setProgress(widgetList, (int) Math.round(vdb.getProgress(w) * elementList.getMaxProgress()));
    	}
    	
    	@GuiCallback("button_close")
    	public void close(Widget w, MouseDownEvent event) {
    		pageMain.transform.doesListenKey = true;
    		pageSelect.transform.doesDraw = false;
    	}
    	
    }
    
    public class CheckCallback {
    	
    	private CheckState state;
    	private String ssid;
    	
    	
    	/**
    	 * Call this before opening the page.
    	 */
    	public void open(String s) {
    		changeState(CheckState.INPUT);
    		pageCheck.transform.doesDraw = true;
    		ssid = s;
    	}
    	
    	/**
    	 * This function must be called ONLY when received action result from sync.
    	 * @param state
    	 */
    	public void changeState(CheckState s) {
    		state = s;
    		Widget yesButton = pageCheck.getWidget("button_yes");
    		Widget inputPw = pageCheck.getWidget("input_pw");
    		Widget textPw = pageCheck.getWidget("text_pw");
    		Widget markState = pageCheck.getWidget("mark_state");
    		Widget markState2 = pageCheck.getWidget("mark_state2");
    		
    		if(state == CheckState.INPUT) {
    			yesButton.transform.doesDraw = true;
    			inputPw.transform.doesDraw = textPw.transform.doesDraw = true;
    			TextBox.get(textPw).content = "";
    			
    			markState.transform.doesDraw = markState2.transform.doesDraw = false;
    		} else {
    			inputPw.transform.doesDraw = textPw.transform.doesDraw = false;
    			if(state == CheckState.TRANSMITTING) {
    				yesButton.transform.doesDraw = false;
        		} else {
        			yesButton.transform.doesDraw = true;
        		}
    			
    			markState.transform.doesDraw = markState2.transform.doesDraw = true;
    			DrawTexture.get(markState2).texture = state.texture;
    		}
    	}
    	
    	@GuiCallback("button_yes")
    	public void onConfirm(Widget w, MouseDownEvent event) {
    		if(state == CheckState.INPUT) {
    			changeState(CheckState.TRANSMITTING);
    			GuiNodeSync.tryLogin(GuiNode.this, ssid, TextBox.get(pageCheck.getWidget("text_pw")).content);
    		} else {
    			pageCheck.transform.doesDraw = false;
    			pageMain.transform.doesListenKey = true;
    		}
    	}
    	
    	@GuiCallback("button_close")
    	public void onClose(Widget w, MouseDownEvent event) {
    		pageCheck.transform.doesDraw = false;
			pageMain.transform.doesListenKey = true;
    	}
    	
    	@GuiCallback("mark_state")
    	public void marked(Widget w, FrameEvent event) {
    		DrawTexture.get(w).color.a = 0.4 + 0.3 * (Math.sin(Minecraft.getSystemTime() / 600.0) + 1);
    	}
    	
    }
    
}
