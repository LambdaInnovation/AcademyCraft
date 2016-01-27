/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.skilltree;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.Extent;
import cn.lambdalib.util.client.font.IFont.FontAlign;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.helper.Color;

import static org.lwjgl.opengl.GL11.*;

class RenderingHelper {
    
    static final Color 
        CRL_BACK = new Color().setColor4d(.4, .4, .4, .5),
        CRL_GLOW = new Color().setColor4d(1, 1, 1, .3);
    
    static final float ZLEV = 15;
    
    public static void drawBox(double x, double y, double width, double height) {
        glPushMatrix();
        glTranslated(0, 0, ZLEV);
        CRL_BACK.bind();
        HudUtils.colorRect(x, y, width, height);
        ACRenderingHelper.drawGlow(x, y, width, height, 3, CRL_GLOW);
        glPopMatrix();
    }
    
    public static void drawTextBox(String str, double x, double y, FontOption option) {
        glEnable(GL_BLEND);
        drawTextBox(str, x, y, 233333, option);
    }
    
    public static void drawTextBox(String str, double x, double y, double limit, FontOption option) {
        final IFont font = Resources.font();
        final Extent extent = font.drawSeperated_Sim(str, limit, option);

        double X0 = x, Y0 = y, MARGIN = Math.min(5, option.fontSize * 0.3);
        
        if(option.align == FontAlign.CENTER) {
            X0 -= extent.width / 2;
        } else if(option.align == FontAlign.RIGHT) {
            X0 -= extent.height;
        }
        
        drawBox(X0, Y0, MARGIN * 2 + extent.width, MARGIN * 2 + extent.height);
        
        glPushMatrix();
        glTranslated(0, 0, ZLEV);
        font.drawSeperated(str, x + MARGIN, Y0 + MARGIN, limit, option);
        glPopMatrix();
    }
    
}
