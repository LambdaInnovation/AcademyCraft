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
package cn.academy.terminal.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cn.academy.core.AcademyCraft;
import cn.academy.core.ModuleCoreClient;
import cn.academy.core.client.Resources;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.core.util.ControlOverrider;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.network.Future.FutureCallback;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.gui.AuxGuiHandler;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.GameTimer;
import cn.liutils.util.helper.KeyHandler;
import cn.liutils.util.helper.KeyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
@Registrant
public class TerminalUI extends AuxGui {
	
	private static final double BALANCE_SPEED = 3; //pixel/ms
	public static final int MAX_MX = 605, MAX_MY = 740;
	
	static final ResourceLocation APP_BACK = tex("app_back"), APP_BACK_HDR = tex("app_back_highlight");
	
	final double SENSITIVITY = 0.7;

	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/terminal.xml"));
	}
	
	LIGui gui;
	
	Widget root;
	
	boolean isActivated = false;
	TerminalMouseHelper helper;
	MouseHelper oldHelper;
	LeftClickHandler clickHandler;
	
	double mouseX, mouseY;
	double buffX, buffY; //Used for rotation judging. Will balance to mouseX and mouseY at the rate of BALANCE_SPEED.
	
	long createTime;
	long lastFrameTime;
	
	int selection = 0;
	int scroll = 0;
	List<Widget> apps = new ArrayList();
	
	boolean isSynced;
	
	public TerminalUI() {
		gui = new LIGui();
		gui.addWidget(root = loaded.getWidget("back").copy());
		
		initGui();
	}
	
	@Override
	public void onAdded() {
		Minecraft mc = Minecraft.getMinecraft();
		oldHelper = mc.mouseHelper;
		mc.mouseHelper = helper = new TerminalMouseHelper();
		
		ModuleCoreClient.dynKeyManager.addKeyHandler("terminal_click", KeyManager.MOUSE_LEFT, clickHandler = new LeftClickHandler());
	}
	
	@Override
	public void onDisposed() {
		Minecraft mc = Minecraft.getMinecraft();
		mc.mouseHelper = oldHelper;
		
		ModuleCoreClient.dynKeyManager.removeKeyHandler("terminal_click");
	}

	@Override
	public boolean isForeground() {
		return false;
	}
	
	@Override
	public boolean isConsistent() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		//Frame update
		selection = (int)((mouseY - 0.01) / MAX_MY * 3) * 3 + (int)((mouseX - 0.01) / MAX_MX * 3);
		
		if(mouseY == 0) {
			mouseY = 20;
			if(scroll > 0) scroll--;
		}
		if(mouseY == MAX_MY) {
			mouseY -= 20;
			if(scroll < getMaxScroll()) scroll++;
		}
		
		//Draw
        Minecraft mc = Minecraft.getMinecraft();
        long time = GameTimer.getTime();
        if(lastFrameTime == 0) lastFrameTime = time;
        long dt = time - lastFrameTime;
        
        mouseX += helper.dx * SENSITIVITY;
        mouseY -= helper.dy * SENSITIVITY;
        mouseX = Math.max(0, Math.min(MAX_MX, mouseX));
        mouseY = Math.max(0, Math.min(MAX_MY, mouseY));
        
        buffX = balance(dt, buffX, mouseX);
        buffY = balance(dt, buffY, mouseY);
        
        helper.dx = helper.dy = 0;
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        float aspect = (float)mc.displayWidth / mc.displayHeight;
        GLU.gluPerspective(50, 
             aspect, 
             1f, 100);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4d(1, 1, 1, 1);
        
        double scale = 1.0 / 310;
        GL11.glTranslated(.35 * aspect, 1.2, -4);
        
        GL11.glTranslated(1, -1.8, 0);
        
        GL11.glRotated(-1.6, 0, 0, 1);
        GL11.glRotated(-18 - 2 * (buffX / MAX_MX - 0.5) + 1 * Math.sin(time / 1000.0), 0, 1, 0);
        GL11.glRotated(7 + 2 * (buffY / MAX_MY - 0.5), 1, 0, 0);
        
        //DEBUG CODE
//        GL11.glPointSize(20);
//        GL11.glColor4d(1, 1, 1, 1);
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        GL11.glBegin(GL11.GL_POINTS);
//        GL11.glVertex3f(0, 0, 0);
//        GL11.glEnd();
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        
        GL11.glTranslated(-1, 1.8, 0);
        
        GL11.glScaled(scale, -scale, scale);
        
        gui.draw(mouseX, mouseY);
        
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glCullFace(GL11.GL_BACK);
	}
	
    private double balance(long dt, double from, double to) {
        double d = to - from;
        return from + Math.min(BALANCE_SPEED * dt, Math.abs(d)) * Math.signum(d);
    }
    
    private void initGui() {
    	EntityPlayer player = Minecraft.getMinecraft().thePlayer;
    	
    	final TerminalData data = TerminalData.get(player);
    	
    	createTime = GameTimer.getTime();
    	
    	updateAppList(data);
    	
    	TextBox.get(root.getWidget("text_username")).content = player.getCommandSenderName();
    	
    	data.querySync(new FutureCallback() {

			@Override
			public void onReady(Object val) {
				if(!isSynced) {
					updateAppList(data);
					root.removeWidget("text_loading");
					root.removeWidget("icon_loading");
					createTime = GameTimer.getTime();
					isSynced = true;
					AcademyCraft.log.debug("Received TerminalUI callback!");
				}
			}
    		
    	});
    	
    	root.getWidget("arrow_up").regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture.get(w).enabled = scroll > 0;
			}
    		
    	});
    	
    	root.getWidget("arrow_down").regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture.get(w).enabled = scroll < getMaxScroll();
			}
    		
    	});
    	
    	root.getWidget("icon_loading").regEventHandlerAtBegin(new FrameEventHandler() {
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				DrawTexture.get(w).color.a = 0.1 + 0.45 * (1 + MathHelper.sin(GameTimer.getTime() / 200.0f));
			}
    	});
    	
    	root.getWidget("text_loading").regEventHandlerAtBegin(FrameEvent.class, (Widget w, FrameEvent event) -> {
    		TextBox.get(w).color.a = 0.1 + 0.45 * (1 + MathHelper.sin(GameTimer.getTime() / 200.0f));
    	});
    }
    
    private void updateAppList(TerminalData data) {
    	for(Widget w : apps)
    		w.dispose();
    	apps.clear();
    	for(Integer i : data.getInstalledApps()) {
    		Widget w = createAppWidget(apps.size(), AppRegistry.INSTANCE.get(i));
    		root.addWidget(w);
    		apps.add(w);
    	}
    	
    	TextBox.get(root.getWidget("text_appcount")).content = 
    		StatCollector.translateToLocalFormatted("ac.gui.terminal.appcount", apps.size());
    	updatePosition();
    }
    
    private void updatePosition() {
    	final double START_X = 65, START_Y = 155, STEP_X = 180, STEP_Y = 180;
    	
    	// Check if scroll is viable
    	int max = getMaxScroll();
    	if(scroll > max) scroll = max;
    	
    	for(Widget w : apps) {
    		w.transform.doesDraw = false;
    	}
    	
    	for(int i = scroll * 3; i < scroll * 3 + 9 && i < apps.size(); ++i) {
    		int order = i - scroll * 3;
    		Widget app = apps.get(i);
    		app.transform.doesDraw = true;
    		app.transform.x = START_X + STEP_X * (order % 3);
    		app.transform.y = START_Y + STEP_Y * (order / 3);
    		app.dirty = true;
    	}
    }
    
    private int getMaxScroll() {
    	int r;
    	if(apps.size() % 3 == 0)
    		r = apps.size() / 3;
    	else r = apps.size() / 3 + 1;
    	return Math.max(0, r - 3);
    }
    
    private Widget getSelectedApp() {
    	int lookup = scroll + selection;
    	return apps.size() <= lookup ? null : apps.get(lookup);
    }
    
    private long getLifetime() {
    	return GameTimer.getTime() - createTime;
    }
    
    private Widget createAppWidget(int id, App app) {
    	Widget ret = root.getWidget("app_template").copy();
    	Widget icon = ret.getWidget("icon");
    	Widget text = ret.getWidget("text");
    	DrawTexture.get(icon).texture = app.getIcon();
    	TextBox.get(text).content = "§L" + app.getDisplayName(); //Enforce bold
    	
    	ret.addComponent(new AppHandler(id, app));
    	
    	return ret;
    }

	private static ResourceLocation tex(String name) {
		return Resources.getTexture("guis/data_terminal/" + name);
	}
	
	@RegACKeyHandler(name = "open_data_terminal", defaultKey = Keyboard.KEY_LMENU)
	@RegEventHandler(Bus.Forge)
	public static KeyHandler keyHandler = new KeyHandler() {
		
		TerminalUI current;
		
		public void onKeyDown() {
			EntityPlayer player = getPlayer();
			TerminalData tData = TerminalData.get(player);
			
			if(tData.isTerminalInstalled()) {
				if(current != null) {
					current.dispose();
					current = null;
				} else {
					current = new TerminalUI();
					AuxGuiHandler.register(current);
				}
			} else {
				player.addChatComponentMessage(new ChatComponentTranslation("ac.terminal.notinstalled"));
			}
		}
		
	};
	
	private class AppHandler extends Component {
		
		final int id;
		final App app;

		DrawTexture drawer;
		TextBox text;
		DrawTexture icon;
		
		boolean lastSelected = true;
		
		public AppHandler(int _id, App _app) {
			super("AppHandler");
			id = _id;
			app = _app;
			
			this.addEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					double mAlpha = MathUtils.wrapd(0.0, 1.0, (getLifetime() - ((id + 1) * 100)) / 400.0);
					boolean selected = getSelectedApp() == w;
					
					if(selected) {
						if(!lastSelected) {
							ACSounds.playClient(Minecraft.getMinecraft().thePlayer, "terminal.select", 0.2f);
						}
						drawer.texture = APP_BACK_HDR;
						
						drawer.zLevel = text.zLevel = icon.zLevel = 40;
						
						drawer.color.a = mAlpha;
						icon.color.a = 0.8 * mAlpha;
						text.color.a = 0.1 + 0.72 * mAlpha;
					} else {
						drawer.texture = APP_BACK;
						
						drawer.zLevel = text.zLevel = icon.zLevel = 10;
						
						drawer.color.a = mAlpha;
						icon.color.a = 0.6 * mAlpha;
						text.color.a = 0.10 + 0.1 * mAlpha;
					}
					
					lastSelected = selected;
				}
				
			});
		}
		
		@Override
		public void onAdded() {
			drawer = DrawTexture.get(widget);
			text = TextBox.get(widget.getWidget("text"));
			icon = DrawTexture.get(widget.getWidget("icon"));
			drawer.color.a = icon.color.a = 0;
			text.color.a = 0.1;
		}
	}
	
	static AppHandler getHandler(Widget w) {
		return w.getComponent("AppHandler");
	}
	
	private class LeftClickHandler extends KeyHandler {
		
		@Override
		public void onKeyDown() {
			Widget app = getSelectedApp();
			if(app != null) {
				AppHandler handler = getHandler(app);
				AppEnvironment env = handler.app.createEnvironment();
				TerminalData data = TerminalData.get(getPlayer());
				
				env.app = handler.app;
				env.terminal = TerminalUI.this;
				
				//ACSounds.playClient(getPlayer(), "terminal.confirm", 0.5f);
				env.onStart();
			}
		}
		
	}
	
}
