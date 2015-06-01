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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cn.academy.core.client.Resources;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.helper.KeyHandler;

/**
 * @author WeAthFolD
 */
@Registrant
public class TerminalUI extends AuxGui {
	
	private static final double BALANCE_SPEED = 3; //pixel/ms
	public static final int MAX_MX = 605, MAX_MY = 740;
	
	final double SENSITIVITY = 0.5;

	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/terminal.xml"));
	}
	
	LIGui gui;
	
	Widget root;
	
	boolean isActivated = false;
	TerminalMouseHelper helper;
	MouseHelper oldHelper;
	
	double mouseX, mouseY;
	double buffX, buffY; //Used for rotation judging. Will balance to mouseX and mouseY at the rate of BALANCE_SPEED.
	
	long lastFrameTime;
	
	public TerminalUI() {
		gui = new LIGui();
		gui.addWidget(root = loaded.getWidget("back"));
		
		initGui();
	}
	
	@Override
	public void onAdded() {
		Minecraft mc = Minecraft.getMinecraft();
		oldHelper = mc.mouseHelper;
		mc.mouseHelper = helper = new TerminalMouseHelper();
	}
	
	@Override
	public void onDisposed() {
		Minecraft mc = Minecraft.getMinecraft();
		mc.mouseHelper = oldHelper;
	}

	@Override
	public boolean isForeground() {
		return true;
	}

	@Override
	public void draw(ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        long time = Minecraft.getSystemTime();
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
        
        double scale;
        if(mc.gameSettings.thirdPersonView == 0) {
            scale = 1.0 / 310;
            GL11.glTranslated(.35 * aspect, 1.2, -4);
        } else {
            scale = 1.0 / 310;
            GL11.glTranslated(.35 * aspect, 1.2, -4);
        }
        
        GL11.glTranslated(1, -1.8, 0);
        
        //GL11.glRotated(20, 1, 0, 0);
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
    	//TODO
    }

	private static ResourceLocation tex(String name) {
		return Resources.getTexture("guis/data_terminal/" + name);
	}
	
	@RegACKeyHandler(name = "open_data_terminal", defaultKey = Keyboard.KEY_U)
	public static KeyHandler keyHandler = new KeyHandler() {
		TerminalUI current;
		
		public void onKeyDown() {
			if(current != null) {
				current.dispose();
			}
			current = new TerminalUI();
			System.out.println("fff");
			register(current);
		}
	};
	
}
