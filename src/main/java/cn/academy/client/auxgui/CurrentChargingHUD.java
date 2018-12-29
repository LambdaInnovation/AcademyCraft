package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.client.render.util.SubArc2D;
import cn.academy.client.render.util.SubArcHandler2D;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class CurrentChargingHUD extends AuxGui {
    
    private static final ResourceLocation mask = Resources.getTexture("effects/em_intensify_mask");
    static final double BLEND_TIME = 0.5, BLEND_OUT_TIME = 0.2;
    
    SubArcHandler2D arcHandler;
    
    public CurrentChargingHUD() {
        arcHandler = new SubArcHandler2D(Resources.ARC_SMALL);
        
        /* Generate charging arc */ {
            int gen = RandUtils.rangei(5, 7);
            while(gen-- > 0) {
                double phi = RandUtils.ranged(0.84, 0.96);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                double size = RandUtils.ranged(25, 30);
                
                SubArc2D arc = arcHandler.generateAt(phi * Math.sin(theta), phi * Math.cos(theta), size);
                arc.life = 233333;
                arc.frameRate = 0.3;
                arc.switchRate = 0.0;
            }
        }
        this.requireTicking = true;
    }

    private double blendTime = -1;
    
    public void startBlend(boolean regen) {
        blendTime = GameTimer.getTime();
        
        arcHandler.clear();
        
        if(regen && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            int gen = RandUtils.rangei(10, 15);
            while(gen-- > 0) {
                double phi = RandUtils.ranged(0.6, 1);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                double size = RandUtils.ranged(35, 40);
                
                SubArc2D arc = arcHandler.generateAt(phi * Math.sin(theta), phi * Math.cos(theta), size);
                arc.life = 25;
                arc.frameRate = 0.3;
                arc.switchRate = 0.2;
            }
        }
    }
    
    private boolean isBlendingOut() {
        return blendTime != -1;
    }

    @Override
    public void onTick() {
        arcHandler.tick();
    }

    @Override
    public void draw(ScaledResolution sr) {
        double width = sr.getScaledWidth_double(), height = sr.getScaledHeight_double();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        
        arcHandler.xScale = width / 2;
        arcHandler.yScale = height / 2;
        
        double mAlpha;
        if(isBlendingOut()) {
            mAlpha = 1 - (GameTimer.getTime() - blendTime) / BLEND_OUT_TIME;
            if(mAlpha < 0) mAlpha = 0;
        } else {
            mAlpha = Math.min(this.getTimeActive() / BLEND_TIME, 1.0);
        }
        
        /* Black Mask */ {
            GL11.glColor4d(0, 0, 0, 0.1 * mAlpha);
            HudUtils.colorRect(0, 0, width, height);
            GL11.glColor4d(1, 1, 1, 1);
        }
        
        /* Blue Mask */ {
            GL11.glColor4d(1, 1, 1, 1 * mAlpha);
            RenderUtils.loadTexture(mask);
            HudUtils.rect(0, 0, width, height);
        }
        
        /* SubArc */ {
            GL11.glColor4d(1, 1, 1, isBlendingOut() ? 0.4 : 0.3);
            GL11.glPushMatrix();
            GL11.glTranslated(width / 2, height / 2, 0);
            arcHandler.drawAll();
            GL11.glPopMatrix();
        }
        
        if(isBlendingOut() && GameTimer.getTime() - blendTime > 1)
            dispose();
    }
    
}