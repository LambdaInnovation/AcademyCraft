/**
 * 
 */
package cn.academy.energy.client.gui;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.energy.block.container.ContainerNode;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;

/**
 * @author WeathFolD
 *
 */
public class GuiNode extends LIGuiContainer {
	
	ResourceLocation TEX = new ResourceLocation("academy:textures/guis/wireless_node.png");

	public GuiNode(ContainerNode c) {
		super(c);
		gui.addWidget(new Page());
	}
	
	private class Page extends Widget {
		public Page() {
			this.alignStyle = AlignStyle.CENTER;
			this.setSize(207.5, 205.333333);
			this.initTexDraw(TEX, 0, 0, 313, 308);
			this.setTexResolution(384, 384);
		}
		
		public void draw(double mx, double my, boolean hov) {
			GL11.glPushMatrix();
			GL11.glTranslated(-.2, 0.5, 0);
			super.draw(mx, my, hov);
			GL11.glPopMatrix();
		}
	}
	
}
