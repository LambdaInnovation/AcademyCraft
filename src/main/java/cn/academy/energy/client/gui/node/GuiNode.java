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
import cn.academy.energy.block.ContainerNode;
import cn.academy.energy.block.TileNode;
import cn.academy.energy.client.gui.node.GuiNodeSync.CheckState;
import cn.academy.energy.client.gui.node.GuiNodeSync.Mat;
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
	

	CheckCallback checker;
	
	// States
	private boolean infoSynced, listSynced;
	
	// Sync info
	private int load;
	private String nodeName = "loading...";
	
	// Sync list info
	private List<Mat> networks = new ArrayList();
    
    public GuiNode(ContainerNode c) {
        super(c);
        container = c;
        tile = c.node;
        create();
        
        GuiNodeSync.doQueryInfo(tile);
    }
    
    public void receivedInitSync(int load, String nodeName) {
    	if(!infoSynced) {
    		infoSynced =  true;
    		
	    	this.load = load;
	    	this.nodeName = nodeName;
	    	
	    	TextBox box = TextBox.get(pageMain.getWidget("text_nodename"));
	    	box.content = nodeName;
	    	box.allowEdit = true;
    	}
    }
    
    public void receivedListSync(List<Mat> ret) {
    	if(!listSynced) {
    		listSynced = true;
    		
    		networks = ret;
    	}
    }
    
    public void receivedActionState(CheckState state) {
    	if(pageCheck.transform.doesDraw) {
    		checker.changeState(state);
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
    	
    	//Event callbacks
    	EventLoader.load(pageMain, new MainCallback());
    	EventLoader.load(pageSelect, new SelectCallback());
    	EventLoader.load(pageCheck, checker = new CheckCallback());
    	
    	//---Main Page
    	ProgressBar.get(pageMain.getWidget("progress_node")).progress = tile.getEnergy() / tile.getMaxEnergy();
    	
    	wrapThunder(pageMain.getWidget("mark_input"), new ThunderCond() {
			@Override
			public boolean shows() {
				return false;
			}
    	});
    	wrapThunder(pageMain.getWidget("mark_output"), new ThunderCond() {
			@Override
			public boolean shows() {
				return false;
			}
    	});
    	wrapThunder(pageMain.getWidget("mark_net"), new ThunderCond() {
			@Override
			public boolean shows() {
				return false;
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
    		list.spacing = 2.0;
    		w.addComponent(list);
    	}
    	
    	//---
    	
    	//---Check page
    	wrapButton(pageCheck.getWidget("button_yes"));
    	
    	//---
    }
    
    private void wrapThunder(Widget thunder, final ThunderCond cond) {
    	thunder.transform.doesDraw = false;
    	thunder.regEventHandler(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				w.transform.doesDraw = cond.shows();
			}
    	});
    }
    
    private void wrapButton(Widget button) {
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

    public static void init() {
    	loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/node.xml"));
    }
    

    
    interface ThunderCond {
    	boolean shows();
    }
    
    public class MainCallback {
    	
        class SSID extends Component {
        	
        	final Mat mat;

    		public SSID(Mat m) {
    			super("SSID");
    			mat = m;
    			
    			this.addEventHandler(new MouseDownHandler() {
    				@Override
    				public void handleEvent(Widget w, MouseDownEvent event) {
    					pageSelect.transform.doesDraw = false;
    					//Open the check window~
    					
    					checker.open(mat);
    				}
    			});
    		}
        	
        }
        
        @GuiCallback("progress_node")
        public void updateProgress(Widget w, FrameEvent event) {
        	ProgressBar.get(w).progress = tile.getEnergy() / tile.getMaxEnergy();
        }
    	
    	@GuiCallback
    	public void drawBlackout(Widget w, FrameEvent event) {
    		if(pageSelect.transform.doesDraw || pageCheck.transform.doesDraw)
    			LIGui.drawBlackout();
    	}
    	
    	@GuiCallback("button_ssid")
    	public void openSSIDPage(Widget button, MouseDownEvent event) {
    		if(listSynced) {
    			//Open the SSID window
    			Widget w = pageSelect.getWidget("list");
    			ElementList list = ElementList.get(w);
    			list.disposeAll(); //clear first
    			
    			for(int i = 0; i < networks.size() && i < 7; ++i) {
    				Widget ssid = pageSelect.getWidget("ssid_template").copy();
    				ssid.addComponent(new SSID(networks.get(i)));
    				TextBox.get(ssid).content = networks.get(i).ssid;
    			}
    		}
    	}
    	
    }
    
    public class SelectCallback {
    	
    }
    
    public class CheckCallback {
    	
    	private CheckState state;
    	private Mat mat;
    	
    	
    	/**
    	 * Call this before opening the page.
    	 */
    	public void open(Mat m) {
    		changeState(CheckState.INPUT);
    		pageCheck.transform.doesDraw = true;
    		mat = m;
    	}
    	
    	/**
    	 * This function must be called ONLY when received action result from sync.
    	 * @param state
    	 */
    	public void changeState(CheckState s) {
    		state = s;
    		Widget yesButton = pageCheck.getWidget("button_yes");
    		Widget inputPw = pageCheck.getWidget("input_pw");
    		if(state == CheckState.INPUT) {
    			yesButton.transform.doesDraw = true;
    			inputPw.transform.doesDraw = true;
    			TextBox.get(inputPw).content = "";
    		} else {
    			inputPw.transform.doesDraw = false;
    			if(state == CheckState.TRANSMITTING) {
    				yesButton.transform.doesDraw = false;
        		} else {
        			yesButton.transform.doesDraw = true;
        		}
    			
    			DrawTexture.get(pageCheck.getWidget("mark_state")).texture = state.texture;
    		}
    	}
    	
    	@GuiCallback("button_yes")
    	public void onConfirm(Widget w, MouseDownEvent event) {
    		if(state == CheckState.INPUT) {
    			changeState(CheckState.TRANSMITTING);
    			GuiNodeSync.tryLogin(GuiNode.this, mat, TextBox.get(pageCheck.getWidget("input_pw")).content);
    		} else {
    			pageCheck.transform.doesDraw = false;
    		}
    	}
    	
    	@GuiCallback("mark_state")
    	public void marked(Widget w, FrameEvent event) {
    		DrawTexture.get(w).color.a = 0.4 + 0.3 * (Math.sin(Minecraft.getSystemTime() / 1000) + 1);
    	}
    	
    }
    
}
