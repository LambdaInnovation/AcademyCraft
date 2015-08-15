package cn.academy.test;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.client.skilltree.GuiSkillTree;
import cn.academy.core.registry.RegACKeyHandler;
import cn.academy.core.util.RangedRayDamage;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Instance;
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
			Minecraft.getMinecraft().displayGuiScreen(new GuiSkillTree(getPlayer()));
		}
	};
	
	
}
