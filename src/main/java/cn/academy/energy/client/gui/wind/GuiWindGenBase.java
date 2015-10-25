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

import org.lwjgl.opengl.GL11;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.core.client.Resources;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.block.wind.ContainerWindGenBase;
import cn.academy.energy.block.wind.TileWindGenBase;
import cn.academy.energy.client.gui.EnergyUIHelper;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiContainer;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.ProgressBar;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;

/**
 * @author WeAthFolD
 */
public class GuiWindGenBase extends LIGuiContainer {
	
	static final ResourceLocation
		T_CORE_OK = Resources.getTexture("guis/button/core_blue"),
		T_CORE_RED = Resources.getTexture("guis/button/core_red");
	
	static LIGui loaded;
	static {
		loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/wind_base.xml"));
	}
	
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
		EventLoader.load(main, this);
		
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
	
	@GuiCallback("core")
	public void changeTexture(Widget w, FrameEvent event) {
		DrawTexture dt = DrawTexture.get(w);
		dt.texture = tile.complete ? T_CORE_OK : T_CORE_RED;
		if(!tile.complete && event.hovering) {
			String text = StatCollector.translateToLocal("ac.gui.wind.structure");
			EnergyUIHelper.drawTextBox(text, -70, -100, 18);
		}
	}
	
	@GuiCallback("prog_fancap")
	public void updateFanCapacity(Widget w, FrameEvent event) {
		ProgressBar bar = ProgressBar.get(w);
		bar.progress = (double) getFanCap() / ModuleEnergy.windgenFan.getMaxDamage();
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
	
	@GuiCallback("prog_speed")
	public void updateSpeed(Widget w, FrameEvent event) {
		ProgressBar bar = ProgressBar.get(w);
		bar.progress = tile.getSimulatedGeneration() / TileWindGenBase.MAX_GENERATION_SPEED;
	}
	
	@GuiCallback("prog_buffer")
	public void updateBuffer(Widget w, FrameEvent event) {
		ProgressBar bar = ProgressBar.get(w);
		bar.progress = tile.getEnergy() / tile.bufferSize;
	}
	
}
