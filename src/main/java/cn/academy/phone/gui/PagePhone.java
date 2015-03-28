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

import org.lwjgl.opengl.GL11;

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

    public PagePhone() {
        super(317, 512);
        
        this.initTexDraw(ClientProps.TEX_GUI_PHONE_BACK, 0, 0, 317, 512);
        this.setTexResolution(512, 512);
    }
    
    @Override
    public void draw(double mx, double my, boolean hover) {
        HudUtils.setZLevel(0);
        super.draw(mx, my, hover);
        
        int[] appList = { 0, 1, 2, 1, 2, 0, 2, 0, 1, 2, 1, 1 };
        
        int highlight = 0;
        
        int cx = 0, cy = 0;
        for(int i = 0; i < appList.length; ++i) {
            int iapp = appList[i];
            App app = AppRegistry.instance.getApp(iapp);
            
            double x = START_X + STEP_X * cx;
            double y = START_Y + STEP_Y * cy;
            
            boolean ht = highlight == i;
            double dz = ht ? 20 : 1;
            
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0, dz);
            GL11.glColor4d(1, 1, 1, ht ? 0.8 : 0.6);
            RenderUtils.loadTexture(ClientProps.TEX_GUI_APP_BG);
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
    }
}
