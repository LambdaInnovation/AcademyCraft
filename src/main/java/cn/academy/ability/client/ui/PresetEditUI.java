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
package cn.academy.ability.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.ctrl.Controllable;
import cn.academy.ability.api.preset.PresetData;
import cn.academy.ability.api.preset.PresetData.Preset;
import cn.academy.ability.api.preset.PresetData.PresetEditor;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class PresetEditUI extends GuiScreen {
	
	static final Color 
		CRL_SKILL = new Color().setColor4i(229, 229, 229, 255),
		CRL_BACK = new Color().setColor4i(49, 49, 49, 255);
	
	static Widget template;
	
	static final double STEP = 125;
	static final long TRANSIT_TIME = 350;
	static final double MAX_ALPHA = 1, MIN_ALPHA = 0.3;
	static final double MAX_SCALE = 0.27, MIN_SCALE = 0.22;
	
	/**
	 * Drawer when nothing happened
	 */
	LIGui foreground = new LIGui();
	
	/**
	 * Drawer of transition
	 */
	LIGui transitor = new LIGui();
	
	static {
		template = CGUIDocLoader.load(new ResourceLocation("academy:guis/preset_edit.xml")).getWidget("template");
	}
	
	@RegGuiHandler
	public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
		
		@SideOnly(Side.CLIENT)
		@Override
		protected GuiScreen getClientGui() {
			return new PresetEditUI();
		}
		
	};
	
	final EntityPlayer player;
	final PresetData data;
	
	// lastActive is the preset ID before the transition.
	int lastActive, active;
	/**
	 * The preset editor of the current active selection. Always not null.
	 */
	PresetEditor editor;
	
	boolean transiting;
	long transitStartTime;
	long deltaTime; //Time that has passed since last transition
	double transitProgress;
	
	Widget skillSelector;
	
	public PresetEditUI() {
		player = Minecraft.getMinecraft().thePlayer;
		data = PresetData.get(player);
		if(!data.isActive()) {
			throw new RuntimeException("Cannot open preset edit gui when data is dirty");
		}
		
		init();
	}
	
	private void init() {
		// Build the pages
		for(int i = 0; i < 4; ++i) {
			Widget normal = createCopy();
			for(Widget w : normal.getDrawList()) {
				// Add for one sub key
				w.addComponent(new HintHandler());
			}
			normal.addComponent(new ForegroundPage(i));
			add(i, foreground, normal);
		}
		
		for(int i = 0; i < 4; ++i) {
			Widget back = createCopy();
			back.addComponent(new TransitPage(i));
			add(i, transitor, back);
		}
		
		resetEditor();
	}
	
	private void resetEditor() {
		updateInfo(foreground);
		updateInfo(transitor);
		
		updatePosForeground();
	}
	
	private Widget createCopy() {
		Widget ret = template.copy();
		for(Widget w : ret.getDrawList()) {
			for(Widget w2 : w.getDrawList())
				w2.regEventHandlerAtBegin(new AlphaAssign());
		}
		return ret;
	}
	
    protected void mouseClicked(int mx, int my, int button) {
    	if(button == 0) {
    		if(!transiting) {
    			foreground.mouseClicked(mx, my, button);
    		}
    	}
    }
	
    public void drawScreen(int mx, int my, float partialTicks) {
    	LIGui.drawBlackout();
    	
    	if(transiting) {
    		updateTransit();
    		transitor.resize(width, height);
    		transitor.draw(mx, my);
    	} else {
    		updatePosForeground();
    		foreground.resize(width, height);
    		foreground.draw(mx, my);
    	}
    }
    
    private double getXFor(int i, int active) {
    	if(i == active) {
    		return width / 2;
    	}
    	return width / 2 + STEP * (i - active);
    }
    
    private double getXFor(int i) {
    	return getXFor(i, active);
    }
    
    private void add(int i, LIGui gui, Widget w) {
    	gui.addWidget("" + i, w);
    }
    
    private Widget get(LIGui gui, int i) {
    	return gui.getWidget("" + i);
    }
    
    // Major control
    private void startTransit(int to) {
    	lastActive = active;
    	active = to;
    	transiting = true;
    	transitStartTime = Minecraft.getSystemTime();
    }
    
    private void finishTransit() {
    	updatePosForeground();
    }
    
    // Foreground page
    private void onEdit(int keyID, int cid) {
    	editor.edit(keyID, cid);
    	getPage(get(foreground, active)).updateInfo();
    }
    
    private void saveEdit() {
    	editor.save();
    }
    
    // Transition page
    private void updateTransit() {
    	deltaTime = Minecraft.getMinecraft().getSystemTime() - transitStartTime;
    	transitProgress = (double)deltaTime / TRANSIT_TIME;
    	if(transitProgress > 1) {
    		transitProgress = 1;
    	}
    	
    	for(int i = 0; i < 4; ++i) {
    		Widget page = get(transitor, i);
    		getPage(page).updatePosition();
    	}
    	
    	if(transitProgress == 1) {
    		transiting = false;
    		finishTransit();
    	}
    }

    // Utils
    private void updateInfo(LIGui gui) {
    	for(int i = 0; i < 4; ++i) {
    		Widget page = get(gui, i);
    		getPage(page).updateInfo();
    	}
    }
    
    private void updatePosForeground() {
    	for(int i = 0; i < 4; ++i) {
    		Widget page = get(foreground, i);
    		getPage(page).updatePosition();
    	}
    }
    
    private abstract class Page extends Component {
    	
    	/**
    	 * Master alpha visited by all sub widgets
    	 */
    	protected double alpha;
    	
    	final int id;
    	
    	public Page(int _id) {
    		super("Page");
    		id = _id;
    	}
    	
    	public void updateInfo() {
    		Preset p = data.getPreset(id);
    		for(int i = 0; i < 4; ++i) {
    			Controllable c = p.getControllable(i);
    			Widget main = widget.getWidget("" + i);
    			DrawTexture.get(main.getWidget("icon")).texture = c == null ? Resources.TEX_EMPTY : c.getHintIcon();
    			TextBox.get(main.getWidget("text")).content = c == null ? "" : c.getHintText();
    		}
    	}
    	
    	public void updatePosition() {
    		widget.transform.x = getXFor(id);
    		widget.dirty = true;
    		
    		alpha = id == active ? MAX_ALPHA : MIN_ALPHA;
    		widget.transform.scale = id == active ? MAX_SCALE : MIN_SCALE;
    		DrawTexture.get(widget).color.a = alpha;
    	}
    	
    	
    }
    
    static Page getPage(Widget w) {
		return w.getComponent("Page");
	}
    
    private class HintHandler extends Component {

		public HintHandler() {
			super("Hint");
			
			addEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					Page page = getPage(w.getWidgetParent());
					DrawTexture dt = DrawTexture.get(w);
					dt.enabled = page.id == active && event.hovering;
					dt.color.a = page.alpha;
				}
				
			});
			
			addEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					Page page = getPage(w.getWidgetParent());
					if(page.id == active) {
						// Open the selector
					} else {
						startTransit(page.id);
					}
				}
				
			});
		}
    	
    }
    
    private class ForegroundPage extends Page {
    	
    	public ForegroundPage(int _id) {
    		super(_id);
    	}
    	
    }
    
    private class TransitPage extends Page {
    	
		public TransitPage(int _id) {
			super(_id);
			
			this.addEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					DrawTexture.get(w).color.a = alpha;
				}
				
			});
		}
		
		@Override
    	public void updatePosition() {
			double x0 = getXFor(id, lastActive), x1 = getXFor(id, active);
			double dx = MathUtils.lerp(x0, x1, transitProgress);
			double scale;
			
    		if(isFrom()) {
    			alpha = MathUtils.lerp(MAX_ALPHA, MIN_ALPHA, transitProgress);
    			scale = MathUtils.lerp(MAX_SCALE, MIN_SCALE, transitProgress);
    		} else if(isTo()) {
    			alpha = MathUtils.lerp(MIN_ALPHA, MAX_ALPHA, transitProgress);
    			scale = MathUtils.lerp(MIN_SCALE, MAX_SCALE, transitProgress);
    		} else {
    			alpha = MIN_ALPHA;
    			scale = MIN_SCALE;
    		}
    		
    		widget.transform.x = dx;
    		widget.transform.scale = scale;
    		
    		DrawTexture.get(widget).color.a = alpha;
    		widget.dirty = true;
    	}
		
		private boolean isFrom() {
			return id == lastActive;
		}
		
		private boolean isTo() {
			return id == active;
		}
    	
    }
    
    private class AlphaAssign extends FrameEventHandler {

		@Override
		public void handleEvent(Widget w, FrameEvent event) {
			double masterAlpha = getPage(w.getWidgetParent().getWidgetParent()).alpha;
			DrawTexture dt = DrawTexture.get(w);
			if(dt != null) {
				dt.color.a = masterAlpha;
			} else {
				TextBox.get(w).color.a = masterAlpha;
			}
		}
    	
    }
	
}
