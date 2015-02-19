/**
 * 
 */
package cn.academy.energy.client.gui;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.energy.block.tile.base.TileUserBase;
import cn.academy.energy.msg.fr.MsgFRInitQuery;
import cn.liutils.api.gui.LIGuiScreen;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.LambdaFont.Align;

/**
 * @author WeathFolD
 *
 */
public class GuiFreqRegulator extends LIGuiScreen {
	
	private static final ResourceLocation TEX = new ResourceLocation("academy:textures/guis/freqreg.png");

	final TileUserBase target;
	static final int[] TEXT_COLOR = { 120, 255, 255, 255 };
	
	//Sync Data
	public boolean synced;
	public Map<String, int[]> channels = new HashMap();
	public String curChannel;
	
	//Pages
	MainPage pageMain;

	public GuiFreqRegulator(TileUserBase _target) {
		target = _target;
		gui.addWidget(pageMain = new MainPage());
		AcademyCraft.netHandler.sendToServer(new MsgFRInitQuery(target));
	}
	
	private class MainPage extends Widget {
		public MainPage() {
			this.setSize(250, 171);
			this.initTexDraw(TEX, 0, 0, 500, 342);
			this.setTexResolution(512, 512);
			this.alignStyle = AlignStyle.CENTER;
		}
		
		@Override
		public void draw(double mx, double my, boolean h) {
			super.draw(mx, my, h);
			RenderUtils.bindColor(TEXT_COLOR);
			drawText(ACLangs.freqReg(), 166, 29, 6, Align.CENTER);
			drawText(ACLangs.frSelectedChannel(), 164, 86, 7, Align.CENTER);
			drawText(ACLangs.frChannelSelect(), 60, 31, 5.7);
			drawText(ACLangs.frCurrentChannel(), 166, 50, 6, Align.CENTER);
			drawText(synced ? 
				(curChannel == null ? ACLangs.notConnected() : curChannel) 
				: ACLangs.loading(), 
				166, 64, 6, Align.CENTER, 90);
			RenderUtils.bindIdentity();
		}
	}
	
    public boolean doesGuiPauseGame()  {
        return false;
    }

	public static void drawText(String text, double x, double y, double size) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size);
	}
	
	public static void drawText(String text, double x, double y, double size, Align align) {
		ACClientProps.FONT_YAHEI_32.draw(text, x, y, size, align);
	}
	
	public static void drawText(String text, double x, double y, double size, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, cst);
	}
	
	public static void drawText(String text, double x, double y, double size, Align align, double cst) {
		ACClientProps.FONT_YAHEI_32.drawAdjusted(text, x, y, size, align, cst);
	}
    
}
