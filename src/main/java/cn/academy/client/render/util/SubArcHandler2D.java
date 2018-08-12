package cn.academy.client.render.util;

import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SubArcHandler2D {

    public final ResourceLocation[] arcs;
    
    List<SubArc2D> list = new LinkedList();
    
    public double frameRate = 1.0, switchRate = 1.0;
    
    public double xScale = 1.0, yScale = 1.0;
    
    public SubArcHandler2D(ResourceLocation[] _arcs) {
        arcs = _arcs;
    }
    
    public SubArc2D generateAt(double x, double y, double size) {
        SubArc2D sa = new SubArc2D(x, y, arcs.length);
        sa.frameRate = frameRate;
        sa.switchRate = switchRate;
        sa.size = size;
        list.add(sa);
        
        return sa;
    }
    
    public void tick() {
        Iterator<SubArc2D> iter = list.iterator();
        while(iter.hasNext()) {
            SubArc2D sa = iter.next();
            if(sa.dead)
                iter.remove();
            else
                sa.tick();
        }
    }
    
    public void clear() {
        list.clear();
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public void drawAll() {
        Iterator<SubArc2D> iter = list.iterator();
        
        GL11.glDepthMask(false);
        GL11.glPushMatrix();
        
        while(iter.hasNext()) {
            SubArc2D arc = iter.next();
            if(!arc.dead && arc.draw) {
                
                GL11.glPushMatrix();
                
                GL11.glTranslated(
                    xScale * arc.x - arc.size / 2, 
                    yScale * arc.y - arc.size / 2, 
                    0);
                
                RenderUtils.loadTexture(arcs[arc.texID]);
                HudUtils.rect(0, 0, arc.size, arc.size);
                
                GL11.glPopMatrix();
            }
        }
        
        GL11.glPopMatrix();
        GL11.glDepthMask(true);
    }

}