/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.Resources;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.Extent;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.helper.Color;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;



public class EnergyUIHelper {
    
    public static final Color 
        CRL_GLOW = new Color().setColor4i(0, 214, 232, 180),
        CRL_BACK = new Color().setColor4i(0, 128, 165, 90),
        CRL_GLOW_MONO = CRL_GLOW.monoize(),
        CRL_BACK_MONO = CRL_BACK.monoize();
    
    public static final ResourceLocation
        BTN_WIFI = Resources.getTexture("guis/button/button_wifi"),
        BTN_WIFI_N = Resources.getTexture("guis/button/button_wifi2");
    
    public static void drawBox(double x, double y, double width, double height) {
        drawBox(x, y, width, height, false);
    }
    
    public static void drawBox(double x, double y, double width, double height, boolean mono) {
        Color back, glow;
        if(mono) {
            back = CRL_BACK_MONO;
            glow = CRL_GLOW_MONO;
        } else {
            back = CRL_BACK;
            glow = CRL_GLOW;
        }

        back.bind();
        HudUtils.colorRect(x, y, width, height);
        
        ACRenderingHelper.drawGlow(x, y, width, height, 3, glow);
    }
    
    public static void drawTextBox(String str, double x, double y, FontOption option) {
        drawTextBox(str, x, y, 233333, option);
    }
    
    public static void drawTextBox(String str, double x, double y, double limit, FontOption option) {
        drawTextBox(str, x, y, limit, option, false);
    }
    
    public static void drawTextBox(String str, double x, double y, double limit, FontOption option, boolean mono) {
        GL11.glEnable(GL11.GL_BLEND);
        final IFont font = Resources.font();

        Extent extent = font.drawSeperated_Sim(str, limit, option);
        if (extent.linesDrawn == 0) {
            extent.height = option.fontSize;
        }
        double X0 = x, Y0 = y, MARGIN = Math.min(5, option.fontSize * 0.3);
        X0 -= extent.width * option.align.lenOffset;

        drawBox(X0, Y0, MARGIN * 2 + extent.width, MARGIN * 2 + extent.height, mono);
        GL11.glPushMatrix();
        GL11.glTranslated(0, 0, 1);
        font.drawSeperated(str, x + MARGIN, Y0 + MARGIN, limit, option);
        GL11.glPopMatrix();
    }
    
}
