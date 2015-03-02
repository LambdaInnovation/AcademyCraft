package cn.academy.core.client.gui;

import net.minecraft.client.gui.GuiScreen;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@RegistrationClass
public class GuiHandlers {
	
	@RegGuiHandler
	public static GuiHandlerBase handlerPresetSettings = new GuiHandlerBase() {
		@Override
		@SideOnly(Side.CLIENT)
		protected GuiScreen getClientGui() {
			return new GuiPresetSettings();
		}
	};

}
