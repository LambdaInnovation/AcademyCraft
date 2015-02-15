/**
 * 
 */
package cn.academy.energy.client.gui;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.impl.ContainerNode;
import cn.academy.energy.block.tile.impl.TileNode;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class GuiNode extends LIGuiContainer {
	
	ResourceLocation TEX = new ResourceLocation("academy:textures/guis/wireless_node.png");
	
	static final int[] COLOR = { 133, 240, 240 };
	
	final ContainerNode node;
	final TileNode tile;

	public GuiNode(ContainerNode c) {
		super(c);
		gui.addWidget(new Page());
		node = c;
		tile = c.node;
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
			
			//44.7 40 35 308 120.67 8.67 181 13
			GL11.glColor4d(1, 1, 1, 1);
			double prog = tile.getEnergy() / tile.getMaxEnergy();
			HudUtils.drawRect(45.8, 54.8, 35, 308, 120.67 * prog, 8.67, 181 * prog, 13);
			GL11.glPopMatrix();
			
			//257 18
			//315 64 18 20
			final double x0 = 170.5, y0 = 11, tw = 18, th = 24;
			if(tile.isConnected()) {
				HudUtils.drawRect(x0, y0, 315, 64, tw / 1.5, th / 1.5, tw, th);
			} else {
				HudUtils.drawRect(x0, y0, 315, 22, tw / 1.5, th / 1.5, tw, th);
			}
			RenderUtils.bindColor(COLOR);
			String channel = tile.isConnected() ? tile.getChannel() : ACLangs.notConnected();
			drawText(channel, 108, 12, 7, Align.CENTER);
			
			RenderUtils.bindIdentity();
		}
	}
	
	private static void drawText(String str, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(str, x, y, size);
	}
	
	private static void drawText(String str, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(str, x, y, size, align);
	}
	
}
