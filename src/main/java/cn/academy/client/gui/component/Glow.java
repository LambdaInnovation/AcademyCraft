package cn.academy.client.gui.component;

import cn.academy.client.render.util.ACRenderingHelper;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.event.FrameEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class Glow extends Component {
    
    public Color color = new Color();
    public double glowSize = 10.0;
    public double zLevel = 0.0;
    public boolean writeDepth = true;

    public Glow() {
        super("Glow");
        
        listen(FrameEvent.class, (w, event) -> {
            if(!writeDepth)
                GL11.glDepthMask(false);
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0, zLevel);
            ACRenderingHelper.drawGlow(0, 0, w.transform.width, w.transform.height, glowSize, color);
            GL11.glPopMatrix();
            GL11.glDepthMask(true);
        });
    }

}