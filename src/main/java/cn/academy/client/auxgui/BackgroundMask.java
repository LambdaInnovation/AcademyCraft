package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.registry.mc.gui.RegAuxGui;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegAuxGui
public class BackgroundMask extends AuxGui {
    
    final ResourceLocation MASK = Resources.preloadMipmapTexture("effects/screen_mask");
    
    final Color CRL_OVERRIDE = new Color(208, 20, 20, 170);
    
    static final double CHANGE_PER_SEC = 1;
    
    double r, g, b, a;
    
    long lastFrame;

    @Override
    public void draw(ScaledResolution sr) {
        double time = GameTimer.getTime();
        
        EntityPlayer player = Minecraft.getMinecraft().player;
        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);
        
        double cr, cg, cb, ca;
        
        Color color = null;
        if(cpData.isOverloaded()) {
            color = CRL_OVERRIDE;
        } else if (cpData.isActivated()) {
            color = aData.getCategory().getColorStyle();
        }
        
        if(color == null) {
            cr = r;
            cg = g;
            cb = b;
            ca = 0;
        } else {
            cr = Colors.i2f(color.getRed());
            cg = Colors.i2f(color.getGreen());
            cb = Colors.i2f(color.getBlue());
            ca = Colors.i2f(color.getAlpha());
        }
        
        if(ca != 0 || a != 0) {
            long dt = lastFrame == 0 ? 0 : (long) (time * 1000) - lastFrame;
            r = balanceTo(r, cr, dt);
            g = balanceTo(g, cg, dt);
            b = balanceTo(b, cb, dt);
            a = balanceTo(a, ca, dt);
            
            GL11.glColor4d(r, g, b, a);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GlStateManager.bindTexture(0);
            RenderUtils.loadTexture(MASK);
            HudUtils.rect(0, 0, sr.getScaledWidth_double(), sr.getScaledHeight_double());
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        } else {
            r = cr;
            g = cg;
            b = cb;
        }
        
        lastFrame = (long) (time * 1000);
    }

    private double balanceTo(double from, double to, long dt) {
        double delta = to - from;
        delta = Math.signum(delta) * Math.min(Math.abs(delta), dt / 1000.0 * CHANGE_PER_SEC);
        return from + delta;
    }
    
}