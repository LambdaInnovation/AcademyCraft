package cn.academy.client.render.util;

import cn.academy.client.render.util.ArcFactory.Arc;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Create one for each entity that you wanna use to draw subArc. Provide the template pre-generated and
 * this class handles everything else.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class SubArcHandler {

    public final Arc[] arcs;
    
    List<SubArc> list = new LinkedList();
    
    public double frameRate = 1.0, switchRate = 1.0;
    
    public SubArcHandler(Arc[] _arcs) {
        arcs = _arcs;
    }
    
    public SubArc generateAt(Vec3d pos) {
        SubArc sa = new SubArc(pos, arcs.length);
        sa.frameRate = frameRate;
        sa.switchRate = switchRate;
        list.add(sa);
        
        return sa;
    }
    
    public void tick() {
        Iterator<SubArc> iter = list.iterator();
        while(iter.hasNext()) {
            SubArc sa = iter.next();
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
        Iterator<SubArc> iter = list.iterator();
        
        GL11.glDepthMask(false);
        while(iter.hasNext()) {
            SubArc arc = iter.next();
            if(!arc.dead && arc.draw) {
                
                GL11.glPushMatrix();
                RenderUtils.glTranslate(arc.pos);
                GL11.glRotated(arc.rotZ, 0, 0, 1);
                GL11.glRotated(arc.rotY, 0, 1, 0);
                GL11.glRotated(arc.rotX, 1, 0, 0);
                
                final double scale = 0.3;
                GL11.glScaled(scale, scale, scale);
                GL11.glTranslated(-arcs[arc.texID].length / 2, 0, 0);
                arcs[arc.texID].draw();
                GL11.glPopMatrix();
            }
        }
        GL11.glDepthMask(true);
    }

}