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
package cn.academy.phone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.generic.client.Resources;
import cn.academy.phone.app.App;
import cn.academy.phone.app.AppRegistry;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.Font.Align;

/**
 * @author WeathFolD
 *
 */
public class PagePhone extends Widget {
    
    static final int PER_LINE = 3, MAX_LINES = 3;
    static final double 
        START_X = 20, START_Y = 42, SIZE = 86, LOGO_SIZE = 81.52, LOGO_YOFF = 7 * (86D / 211), LOGO_XOFF = 5 * (86D / 211),
        STEP_X = 96, STEP_Y = 110;
    
    static final int MAX_MX = GuiPhone.MAX_MX, MAX_MY = GuiPhone.MAX_MY;
    
    final GuiPhone guiPhone;
    int[] appList;
    int progress;
    int maxProgress;
    
    public PagePhone(GuiPhone gui) {
        guiPhone = gui;
        this.addComponent(new DrawTexture().setTex(Resources.TEX_PHONE_BACK));
        transform.setSize(317, 512);
        
        this.regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
		        HudUtils.setZLevel(0);
		        
		        double mx = event.mx, my = event.my;
		        long time = Minecraft.getSystemTime();
		        int lines = fldiv(appList.length, PER_LINE); //Actually lines-1
		        int hLine = GenericUtils.min(lines, (int) (MAX_LINES * (my / MAX_MY)), MAX_LINES - 1), 
		            hColumn = GenericUtils.min((hLine == lines ? 
		                    appList.length - lines * PER_LINE : PER_LINE) - 1, 
		                    (int) (PER_LINE * (mx / MAX_MX)), PER_LINE - 1);
		        int highlight = progress * PER_LINE + hLine * PER_LINE + hColumn;
		        
		        //Logic update
		        if(guiPhone.mouseY == MAX_MY) {
		            progress = Math.min(maxProgress, progress + 1);
		            guiPhone.mouseY -= 50;
		        } else if(guiPhone.mouseY == 0) {
		            progress = Math.max(0, progress - 1);
		            guiPhone.mouseY += 50;
		        }
		        
		        //draw App
		        int cx = 0, cy = 0;
		        for(int i = progress * PER_LINE; i < appList.length; ++i) {
		            int iapp = appList[i];
		            App app = AppRegistry.instance.getApp(iapp);
		            
		            double x = START_X + STEP_X * cx;
		            double y = START_Y + STEP_Y * cy;
		            
		            boolean ht = highlight == i;
		            double dz = ht ? 10 : 1;
		            
		            GL11.glPushMatrix();
		            GL11.glTranslated(x, y, dz);
		            GL11.glColor4d(1, 1, 1, ht ? 1 : 0.6);
		            RenderUtils.loadTexture(Resources.TEX_PHONE_APP_BG);
		            HudUtils.drawRect(0, 0, SIZE, SIZE);
		            
		            RenderUtils.loadTexture(app.getIcon());
		            HudUtils.drawRect(LOGO_XOFF, LOGO_YOFF, LOGO_SIZE, LOGO_SIZE);
		            
		            String name = app.getDisplayName();
		            float fsize = 12f;
		            Resources.font().draw(name, SIZE / 2, SIZE + 2, fsize, 0xffffff, Align.CENTER);
		            GL11.glPopMatrix();
		            
		            ++cx;
		            if(cx == 3) {
		                cx = 0;
		                ++cy;
		            }
		            if(cx + cy * 3 == 9) break;
		        }
		        
		        drawUpdate(time);
		        
		        if(progress < maxProgress) {
		            drawDownArrow();
		        }
			}
        	
        });
        
        appList = this.getInstalledAppList(Minecraft.getMinecraft().thePlayer);
        maxProgress = Math.max(0, udiv(appList.length, PER_LINE) - MAX_LINES);
    }
    
    private void drawUpdate(long time) {
        final double size = 100;
        GL11.glPushMatrix();
        GL11.glTranslated(230, 360, 0);
        GL11.glColor4d(1, 1, 1, 0.6);
        final double scale = 0.9;
        GL11.glScaled(scale, scale, scale);
        HudUtils.setZLevel(15);
        
        GL11.glPushMatrix();
        RenderUtils.loadTexture(Resources.TEX_PHONE_SYNC);
        GL11.glTranslated(size / 2, size / 2, 0);
        GL11.glRotated(time / 200.0, 0, 0, 1);
        GL11.glTranslated(-size / 2, -size / 2, 0);
        HudUtils.drawRect(0, 0, size, size);
        GL11.glPopMatrix();
        
        GL11.glPushMatrix();
        GL11.glTranslated(0, 5 * Math.sin(time / 1000.0), 0);
        RenderUtils.loadTexture(Resources.TEX_PHONE_SYNC_MASK);
        HudUtils.setZLevel(19);
        HudUtils.drawRect(0, 0, size, size);
        GL11.glPopMatrix();
        
        HudUtils.setZLevel(27);
        Resources.font().draw("Loading...", 30, 15, 15, 0xffffff);
        
        drawHint(-375, -400, Resources.TEX_PHONE_HINT_ML, "Open App");
        drawHint(-375, -340, Resources.TEX_PHONE_HINT_MR, "Quit");
        
        GL11.glPopMatrix();
    }
    
    private void drawHint(double x, double y, ResourceLocation base, String text) {
        GL11.glPushMatrix();
        HudUtils.setZLevel(0);
        GL11.glTranslated(x, y, 10);
        float scale = .5f;
        GL11.glScalef(scale, scale, scale);
        RenderUtils.loadTexture(base);
        HudUtils.drawRect(0, 0, 256, 256);
        Resources.font().draw(text, 155, 122, 28, 0xffffff, Align.CENTER);
        GL11.glPopMatrix();
    }
    
    private void drawDownArrow() {
        RenderUtils.loadTexture(Resources.TEX_PHONE_ARROW);
        HudUtils.setZLevel(2);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glColor4d(1, 1, 1, 0.5);
        HudUtils.drawRect(120, 360, 80, 80);
        GL11.glColor4d(1, 1, 1, 1);
    }
    
    public int[] getInstalledAppList(EntityPlayer player) {
        return new int[] { 0, 1, 2, 1, 2, 0, 2, 0, 2, 1 };
    }
    
    int fldiv(int a, int b) {
        return a % b == 0 ? (a / b - 1) : (a / b);
    }
    
    int udiv(int a, int b) {
        return a % b == 0 ? (a / b) : (a / b + 1);
    }
}
