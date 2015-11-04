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
package cn.academy.crafting.client.gui;

import org.lwjgl.opengl.GL11;

import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.block.ContainerImagFusor;
import cn.academy.crafting.block.TileImagFusor;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.cgui.gui.LIGui;
import cn.lambdalib.cgui.gui.LIGuiContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.annotations.GuiCallback;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.EventLoader;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.helper.Color;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class GuiImagFusor extends LIGuiContainer {
	
	static LIGui loaded;
	
	public static void init() {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/imagfusor.xml"));
	}
	
	final TileImagFusor tile;
	
	Widget page;

	public GuiImagFusor(ContainerImagFusor c) {
		super(c);
		tile = c.tile;
		load();
	}
	
	private void wrapButton(final Widget but) {
		final Color idle = new Color(1, 1, 1, 0.3), hover = new Color(1, 1, 1, 1);
		but.listen(FrameEvent.class, (w, event) -> 
		{
			DrawTexture.get(but).color = event.hovering ? hover : idle;
		});
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y) {
		GL11.glPushMatrix();
		//Notice: We used a hack to get rid of MC's offset and use absolute offset.
		GL11.glTranslated(-this.guiLeft, -this.guiTop, 0);
		
		 Widget widget = gui.getTopWidget(x, y);
		 if(widget != null) {
			 String text = null;
			 if(widget.getName().equals("progress_imag")) {
				 text = tile.getEnergy() + "/" + tile.getMaxEnergy() + " IF";
			 } else if(widget.getName().equals("progress_proj")) {
				 text = tile.getLiquidAmount() + "/" + tile.getTankSize() + " mB";
			 }
			 
			 if(text != null) {
				 EnergyUIHelper.drawTextBox(text, x + 5, y + 2, 9);
			 }
		 }
		 
		 GL11.glPopMatrix();
	}
	
	private void load() {
		gui.addWidget(page = loaded.getWidget("window_main"));
		
		EventLoader.load(page, new Handler());
	}
	
	public class Handler {
		
		ProgressBar progressProduct, progressProj, progressImag;
		
		public Handler() {
			progressProduct = ProgressBar.get(page.getWidget("progress_pro"));
			progressProj = ProgressBar.get(page.getWidget("progress_proj"));
			progressImag = ProgressBar.get(page.getWidget("progress_imag"));
			
			EnergyUIHelper.initNodeLinkButton(tile, page.getWidget("btn_link"));
		}
		
		@GuiCallback
		public void frameUpdate(Widget w, FrameEvent event) {
			progressProduct.progress = tile.getWorkProgress();
			progressProj.progress = (double) tile.getLiquidAmount() / tile.getTankSize();
			progressImag.progress = tile.getEnergy() / tile.getMaxEnergy();
			
			String str;
			IFRecipe recipe = tile.getCurrentRecipe();
			if(recipe == null) {
				str = "";
			} else {
				str = "" + recipe.consumeLiquid;
			}
			
			TextBox.get(w.getWidget("text_req")).content = str;
		}
	}

}
