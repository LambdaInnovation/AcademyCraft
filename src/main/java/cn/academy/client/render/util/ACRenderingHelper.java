package cn.academy.client.render.util;

import cn.academy.Resources;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

/**
 * Some drawing utils.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class ACRenderingHelper {
    
    public static ResourceLocation
        GLOW_L = glowtex("left"),
        GLOW_R = glowtex("right"),
        GLOW_U = glowtex("up"),
        GLOW_D = glowtex("down"),
        GLOW_RU = glowtex("ru"),
        GLOW_RD = glowtex("rd"),
        GLOW_LU = glowtex("lu"),
        GLOW_LD = glowtex("ld");

    public static void drawGlow(double x, double y, double width, double height, double size, Color glowColor) {
        Colors.bindToGL(glowColor);

        final double s = size;
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        gdraw(GLOW_L,  x - s,       y,           s,      height);
        gdraw(GLOW_R,  x + width, y,           s,      height);
        gdraw(GLOW_U,  x,             y - s,       width, s);
        gdraw(GLOW_D,  x,             y + height, width, s);
        gdraw(GLOW_RU, x + width, y - s,       s,      s);
        gdraw(GLOW_RD, x + width, y + height, s,      s);
        gdraw(GLOW_LU, x - s,       y - s,       s,      s);
        gdraw(GLOW_LD, x - s,       y + height, s,      s);
    }
    
    public static boolean isThePlayer(EntityPlayer p) {
        return p.equals(Minecraft.getMinecraft().player);
    }
    
    public static double getHeightFix(EntityPlayer p) {
    //    return isThePlayer(p) ? 0.0 : 1.6;
        // !1.12: Difference of reference coordinates has been fixed by mojang
        return 1.6;
    }
    
    /**
     * Draws a circular progress bar at (0, 0) with radius 1
     */
    public static void drawCircularProgbar(ResourceLocation texture, double progress) {
        
        progress *= 360;
        if(progress > 360)
            progress %= 360;
        
        GL11.glPushMatrix();
        RenderUtils.loadTexture(texture);
        for(int i = 0; i < 4; ++i) {
            double angle = Math.min(90, progress - 90 * i);
            if(angle <= 0)
                break;
            double u1, v1;
            GL11.glBegin(GL11.GL_TRIANGLES);
            
            if(angle <= 45) {
                u1 = Math.tan(MathUtils.toRadians(angle));
                v1 = 0;
            } else {
                u1 = 1;
                v1 = 0;
                
                double x = Math.tan(MathUtils.toRadians(90 - angle));
                RenderUtils.glVertexAndUV(1, -1, 0, 1, 0);
                RenderUtils.glVertexAndUV(0, 0, 0, 0, 1);
                RenderUtils.glVertexAndUV(1, -x, 0, 1, 1 - x);
            }
            
            RenderUtils.glVertexAndUV(0, -1, 0, 0, 0);
            RenderUtils.glVertexAndUV(0, 0, 0, 0, 1);
            RenderUtils.glVertexAndUV(u1,  -1 -v1, 0, u1, v1);

            GL11.glEnd();

            GL11.glRotated(90, 0, 0, 1);
        }
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void lineSegmentGlow(double x0, double y0, double x1, double y1, float width) {
        HudUtils.loadTexture(Resources.TEX_GLOW_LINE);
        dirQuad(x0, y0, x1, y1, width);
    }
    
    public static void lineSegment(double x0, double y0, double x1, double y1, float width) {
        boolean pre = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        dirQuad(x0, y0, x1, y1, width);

        if(pre) GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void dirQuad(double x0, double y0, double x1, double y1, float width) {
        float hw = width / 2;
        double dy = y1 - y0, dx = x1 - x0, len = Math.sqrt(dy * dy + dx * dx);
        double theta = MathUtils.toDegrees(Math.atan2(dy, dx));

        GL11.glPushMatrix();
        GL11.glTranslated(x0, y0, 0);
        GL11.glRotated(theta, 0, 0, 1);
        GL11.glBegin(GL11.GL_QUADS);
        RenderUtils.glVertexAndUV(0, -hw, 0, 0, 0);
        RenderUtils.glVertexAndUV(0, hw, 0, 0, 1);
        RenderUtils.glVertexAndUV(len, hw, 0, 1, 1);
        RenderUtils.glVertexAndUV(len, -hw, 0, 1, 0);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private static void gdraw(ResourceLocation tex, double x, double y, double width, double height) {
        HudUtils.loadTexture(tex);
        HudUtils.rect(x, y, width, height);
    }
    
    private static ResourceLocation glowtex(String path) {
        return Resources.getTexture("guis/glow_" + path);
    }
    
}