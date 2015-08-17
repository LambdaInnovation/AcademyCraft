package cn.academy.test;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.client.skilltree.GuiSkillTree;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.liutils.util.helper.KeyHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Tests
 */
@Registrant
public class Tests {
	
	@RegACKeyHandler(name = "miku", defaultKey = Keyboard.KEY_K)
	public static KeyHandler key = new KeyHandler() {
		@Override
		@SideOnly(Side.CLIENT)
		public void onKeyDown() {
			Minecraft.getMinecraft().displayGuiScreen(new GuiSkillTree(getPlayer(), DeveloperType.PORTABLE));
		}
	};
	
	
}
