/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.component;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACRenderingHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.CGUIEditor;
import cn.liutils.util.helper.Color;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegInit
public class Glow extends Component {
	
	public Color color = new Color();
	public double glowSize = 10.0;
	public double zLevel = 0.0;
	public boolean writeDepth = true;
	
	public static void init() {
		// Register for editing
		CGUIEditor.addComponent(new Glow());
	}
	
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
