package cn.academy.ability.client.ui;

import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.Resources;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.auxgui.AuxGui;
import cn.lambdalib2.util.auxgui.AuxGuiRegistry.RegAuxGui;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.GameTimer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegAuxGui
public class BackgroundMask extends AuxGui {
    
    final ResourceLocation MASK = Resources.preloadMipmapTexture("effects/screen_mask");
    
    final Color CRL_OVERRIDE = new Color().setColor4i(208, 20, 20, 170);
    
    static final double CHANGE_PER_SEC = 1;
    
    double r, g, b, a;
    
    long lastFrame;

    @Override
    public boolean isForeground() {
        return false;
    }

    @Override
    public void draw(ScaledResolution sr) {
        long time = GameTimer.getTime();
        
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
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
            cr = color.r;
            cg = color.g;
            cb = color.b;
            ca = color.a;
        }
        
        if(ca != 0 || a != 0) {
            long dt = lastFrame == 0 ? 0 : time - lastFrame;
            r = balanceTo(r, cr, dt);
            g = balanceTo(g, cg, dt);
            b = balanceTo(b, cb, dt);
            a = balanceTo(a, ca, dt);
            
            GL11.glColor4d(r, g, b, a);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            RenderUtils.loadTexture(MASK);
            HudUtils.rect(0, 0, sr.getScaledWidth_double(), sr.getScaledHeight_double());
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        } else {
            r = cr;
            g = cg;
            b = cb;
        }
        
        lastFrame = time;
    }

    private double balanceTo(double from, double to, long dt) {
        double delta = to - from;
        delta = Math.signum(delta) * Math.min(Math.abs(delta), dt / 1000.0 * CHANGE_PER_SEC);
        return from + delta;
    }
    
}