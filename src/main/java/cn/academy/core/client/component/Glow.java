/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.component;

import cn.academy.core.client.ACRenderingHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
public class Glow extends Component {
    
    public Color color = new Color();
    public double glowSize = 10.0;
    public double zLevel = 0.0;
    public boolean writeDepth = true;
    
    public static Glow get(Widget w) {
        return w.getComponent("Glow");
    }

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
