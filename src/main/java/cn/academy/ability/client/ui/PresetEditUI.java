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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.data.PresetData.PresetEditor;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.GameTimer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class PresetEditUI extends GuiScreen {
	
	static final Color 
		CRL_BACK = new Color().setColor4i(49, 49, 49, 160),
		CRL_WHITE = new Color(1, 1, 1, 0.6);
	
	static LIGui loaded;
	static Widget template;
	
	static final double STEP = 125;
	static final long TRANSIT_TIME = 350;
	static final double MAX_ALPHA = 1, MIN_ALPHA = 0.3;
	static final double MAX_SCALE = 0.27, MIN_SCALE = 0.22;
	
	/**
	 * Dummy skill used to cancel the binding.
	 */
	static final Skill cancelBinding = new Skill("cancel", -1) {
		@Override
		public ResourceLocation getHintIcon() {
			return new ResourceLocation("academy:textures/guis/preset_settings/cancel.png");
		}

		@Override
		public String getHintText() {
			return "";
		}
	};
	
	/**
	 * Drawer when nothing happened
	 */
	LIGui foreground = new LIGui();
	
	/**
	 * Drawer of transition
	 */
	LIGui transitor = new LIGui();
	
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/preset_edit.xml"));
		template = loaded.getWidget("template");
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
	
	Widget selector;
	
	public PresetEditUI() {
		player = Minecraft.getMinecraft().thePlayer;
		data = PresetData.get(player);
		if(!data.isActive()) {
			throw new RuntimeException("Cannot open preset edit gui when data is dirty");
		}
		
		init();
	}
	
	public void onGuiClosed() {
		editor.save();
	}
	
	private String local(String key) {
		return StatCollector.translateToLocal("ac.gui.preset_edit." + key);
	}
	
	private void init() {
		foreground.addWidget(loaded.getWidget("background").copy());
		transitor.addWidget(loaded.getWidget("background").copy());
		
		// Build the pages
		for(int i = 0; i < 4; ++i) {
			Widget normal = createCopy();
			TextBox.get(normal.getWidget("title")).setContent( local("tag") + (i + 1) );
			
			for(int j = 0; j < 4; ++j) {
				normal.getWidget("" + j).addComponent(new HintHandler(j));
			}
			normal.addComponent(new ForegroundPage(i));
			add(i, foreground, normal);
		}
		
		for(int i = 0; i < 4; ++i) {
			Widget back = createCopy();
			back.addComponent(new TransitPage(i));
			add(i, transitor, back);
		}
		
		resetAll();
	}
	
	private void resetAll() {
		updateInfo(foreground);
		updateInfo(transitor);
		
		updatePosForeground();
		
		updateEditor();
	}
	
	private void updateEditor() {
		if(editor != null)
			editor.save();
		editor = data.createEditor(active);
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
    		transitor.draw(-1, -1);
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
    	updateInfo(transitor);
    	
    	lastActive = active;
    	active = to;
    	transiting = true;
    	transitStartTime = GameTimer.getAbsTime();
    }
    
    private void finishTransit() {
    	updatePosForeground();
    	updateEditor();
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
    	deltaTime = GameTimer.getAbsTime() - transitStartTime;
    	
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
    		byte[] pdata = (id == active && editor != null) ? editor.display : p.getData();
    		
    		for(int i = 0; i < 4; ++i) {
    			Category cat = AbilityData.get(player).getCategory();
    			Controllable c = cat.getControllable(pdata[i]);
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

    	final int keyid;
    	
		public HintHandler(int _keyid) {
			super("Hint");
			keyid = _keyid;
			
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
					if(selector != null && !selector.disposed) {
						selector.dispose();
						selector = null;
					} else if(page.id == active) {
						// Open the selector
						selector = new Selector(keyid);
						selector.transform.setPos(foreground.mouseX, foreground.mouseY);
						foreground.addWidget(selector);
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
    
    private class Selector extends Widget {
    	final int MAX_PER_ROW = 4;
    	final double MARGIN = 2.5, SIZE = 15, STEP = SIZE + 3;
    	
    	List<Skill> available = new ArrayList();
    	final int keyid;
    	
    	double width, height;
    	
    	public Selector(int _keyid) {
    		keyid = _keyid;
    		
    		AbilityData aData = AbilityData.get(player);
    		Category c = aData.getCategory();
    		
    		available.add(cancelBinding);
    		for(Skill s : aData.getControllableSkillList()) {
    			int cid = s.getControlID();
    			if(!editor.hasMapping(cid)) {
    				available.add(s);
    			}
    		}
    		
    		height = MARGIN * 2 + SIZE + STEP * (ldiv(available.size(), MAX_PER_ROW) - 1);
    		width = available.size() < MAX_PER_ROW ? 
    			MARGIN * 2 + SIZE + STEP * (available.size() - 1) : 
    			MARGIN * 2 + SIZE + STEP * (MAX_PER_ROW - 1);
    		
    		transform.setSize(width, height);
    		
    		// Build the window and the widget
    		regEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					CRL_WHITE.bind();
					ACRenderingHelper.drawGlow(0, 0, width, height, 1, CRL_WHITE);
					
					CRL_BACK.bind();
					HudUtils.colorRect(0, 0, width, height);
					
					Widget hovering = foreground.getHoveringWidget();
					if(hovering != null && hovering.getName().contains("_sel")) {
						SelHandler sh = hovering.getComponent("_sel");
						Font.font.draw(sh.skill.getHintText(), 0, -10, 10, 0xffffff);
					}
					
					GL11.glColor4d(1, 1, 1, 1);
				}
    			
    		});
    		
    		// Build all the skills that can be set
    		for(int i = 0; i < available.size(); ++i) {
    			int row = i / MAX_PER_ROW, col = i % MAX_PER_ROW;
    			final Skill skill = available.get(i);
    			Widget single = new Widget();
    			single.transform.setPos(MARGIN + col * STEP, MARGIN + row * STEP);
    			single.transform.setSize(SIZE, SIZE);
    			
    			DrawTexture tex = new DrawTexture().setTex(available.get(i).getHintIcon());
    			single.addComponent(tex);
    			single.addComponent(new Tint());
    			single.addComponent(new SelHandler(skill));
    			addWidget("_sel" + i, single);
    		}
    	}
    	
    	private class SelHandler extends Component {
    		
    		final Skill skill;

			public SelHandler(Skill _skill) {
				super("_sel");
				skill = _skill;
				this.addEventHandler(new MouseDownHandler() {

					@Override
					public void handleEvent(Widget w, MouseDownEvent event) {
						onEdit(keyid, skill.getControlID());
						Selector.this.dispose();
					}
    				
    			});
			}
    		
    	}
    }
    
    private int ldiv(int a, int b) {
    	return a % b == 0 ? a / b : a / b + 1;
    }
	
}
