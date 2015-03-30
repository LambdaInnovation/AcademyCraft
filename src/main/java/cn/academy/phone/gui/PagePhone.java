/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import cn.academy.generic.client.ClientProps;
import cn.academy.phone.app.App;
import cn.academy.phone.app.AppRegistry;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class PagePhone extends Widget {
    
    static final int PER_LINE = 3, MAX_LINES = 3;
    static final int START_X = 20, START_Y = 42, SIZE = 86, STEP_X = 96, STEP_Y = 110;
    
    static final int MAX_MX = GuiPhone.MAX_MX, MAX_MY = GuiPhone.MAX_MY;

    public PagePhone() {
        super(317, 512);
        
        this.initTexDraw(ClientProps.TEX_PHONE_BACK, 0, 0, 317, 512);
        this.setTexResolution(512, 512);
    }
    
    @Override
    public void draw(double mx, double my, boolean hover) {
        HudUtils.setZLevel(0);
        super.draw(mx, my, hover);
        long time = Minecraft.getSystemTime();
        
        int[] appList = { 0, 1, 2, 1, 2, 0, 2, 0 };
        int lines = fldiv(appList.length, PER_LINE); //Actually lines-1
        
        int hLine = Math.min(lines, Math.min((int) (MAX_LINES * (my / MAX_MY)), MAX_LINES - 1)), 
            hColumn = Math.min((hLine == lines ? appList.length - lines * PER_LINE : PER_LINE) - 1, 
                    Math.min((int) (PER_LINE * (mx / MAX_MX)), PER_LINE - 1));
        int highlight = hLine * 3 + hColumn;
        
        int cx = 0, cy = 0;
        for(int i = 0; i < appList.length; ++i) {
            int iapp = appList[i];
            App app = AppRegistry.instance.getApp(iapp);
            
            double x = START_X + STEP_X * cx;
            double y = START_Y + STEP_Y * cy;
            
            boolean ht = highlight == i;
            double dz = ht ? 10 : 1;
            
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0, dz);
            GL11.glColor4d(1, 1, 1, ht ? 1 : 0.6);
            RenderUtils.loadTexture(ClientProps.TEX_PHONE_APP_BG);
            HudUtils.drawRect(x, y, SIZE, SIZE);
            
            String name = app.getDisplayName();
            float fsize = 12f;
            ClientProps.font().draw(name, x + SIZE / 2, y + SIZE + 2, fsize, Align.CENTER);
            GL11.glPopMatrix();
            
            ++cx;
            if(cx == 3) {
                cx = 0;
                ++cy;
            }
            if(cx + cy * 3 == 9) break;
        }
        
        drawUpdate(time);
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
        RenderUtils.loadTexture(ClientProps.TEX_PHONE_SYNC);
        GL11.glTranslated(size / 2, size / 2, 0);
        GL11.glRotated(time / 200.0, 0, 0, 1);
        GL11.glTranslated(-size / 2, -size / 2, 0);
        HudUtils.drawRect(0, 0, size, size);
        GL11.glPopMatrix();
        
        GL11.glPushMatrix();
        GL11.glTranslated(0, 5 * Math.sin(time / 1000.0), 0);
        RenderUtils.loadTexture(ClientProps.TEX_PHONE_SYNC_MASK);
        HudUtils.setZLevel(19);
        HudUtils.drawRect(0, 0, size, size);
        GL11.glPopMatrix();
        
        HudUtils.setZLevel(27);
        ClientProps.font().draw("Loading...", 30, 15, 15);
        
        GL11.glPopMatrix();
    }
    
    int fldiv(int a, int b) {
        return a % b == 0 ? (a / b - 1) : (a / b);
    }
}
