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
package cn.academy.energy.client.gui.wind;

import cn.lambdalib.cgui.gui.WidgetContainer;
import cn.lambdalib.cgui.xml.CGUIDocument;
import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.core.client.Resources;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.block.wind.ContainerWindGenBase;
import cn.academy.energy.block.wind.TileWindGenBase;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.CGuiScreenContainer;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeAthFolD
 */
public class GuiWindGenBase extends CGuiScreenContainer {
	
	static final ResourceLocation
		T_CORE_OK = Resources.getTexture("guis/button/core_blue"),
		T_CORE_RED = Resources.getTexture("guis/button/core_red");
	
	static final WidgetContainer loaded = CGUIDocument.panicRead(new ResourceLocation("academy:guis/wind_base.xml"));
	
	final TileWindGenBase tile;
	
	Widget main;

	public GuiWindGenBase(ContainerWindGenBase c) {
		super(c);
		
		tile = c.tile;
		initWidgets();
	}
	
	void initWidgets() {
		main = loaded.getWidget("main").copy();
		
		EnergyUIHelper.initNodeLinkButton(tile, main.getWidget("btn_link"));

		main.getWidget("core").listen(FrameEvent.class, (w, event) -> {
			DrawTexture dt = DrawTexture.get(w);
			dt.texture = tile.complete ? T_CORE_OK : T_CORE_RED;
			if(!tile.complete && event.hovering) {
				String text = StatCollector.translateToLocal("ac.gui.wind.structure");
				EnergyUIHelper.drawTextBox(text, -70, -100, 18);
			}
		});

		main.getWidget("prog_fancap").listen(FrameEvent.class, (w, e) -> {
			ProgressBar bar = ProgressBar.get(w);
			bar.progress = (double) getFanCap() / ModuleEnergy.windgenFan.getMaxDamage();
		});

		main.getWidget("prog_speed").listen(FrameEvent.class, (w, e) -> {
			ProgressBar bar = ProgressBar.get(w);
			bar.progress = tile.getSimulatedGeneration() / TileWindGenBase.MAX_GENERATION_SPEED;
		});

		main.getWidget("prog_buffer").listen(FrameEvent.class, (w, e) -> {
			ProgressBar bar = ProgressBar.get(w);
			bar.progress = tile.getEnergy() / tile.bufferSize;
		});
		
		gui.addWidget(main);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int x, int y) {
		GL11.glPushMatrix();
		GL11.glTranslated(-guiLeft, -guiTop, 0);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		Widget w = gui.getTopWidget(x, y);
		if(w != null) {
			String text = null;
			switch(w.getName()) {
			case "prog_fancap":
				text = getFanCap() + "/" + ModuleEnergy.windgenFan.getMaxDamage();
				break;
			case "prog_speed":
				text = tile.getSimulatedGeneration() + "IF/T";
				break;
			case "prog_buffer":
				text = String.format("%.1f/%.1fIF", tile.getEnergy(), tile.bufferSize);
				break;
			}
			
			if(text != null) {
				//int offsetX = -160, offsetY = -45;
				EnergyUIHelper.drawTextBox(text, x + 5, y, 10);
			}
		}
		
		GL11.glPopMatrix();
	}
	
	private int getFanCap() {
		if(tile.mainTile == null) {
			return 0;
		} else {
			ItemStack stack = tile.mainTile.getStackInSlot(0);
			if(stack != null && stack.getItem() == ModuleEnergy.windgenFan) {
				return stack.getMaxDamage() - stack.getItemDamage();
			} else {
				return 0;
			}
		}
	}
	
}
