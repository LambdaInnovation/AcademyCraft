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
package cn.academy.terminal.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import cn.academy.core.item.ACItem;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.client.TerminalInstallEffect;
import cn.academy.terminal.client.TerminalInstallerRenderer;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.liutils.api.gui.AuxGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class ItemTerminalInstaller extends ACItem {
	
	@SideOnly(Side.CLIENT)
	@RegItem.Render
	public static TerminalInstallerRenderer renderer;

	public ItemTerminalInstaller() {
		super("terminal_installer");
		this.bFull3D = true;
		this.maxStackSize = 1;
	}
	
	@Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		TerminalData tData = TerminalData.get(player);
		if(tData.isTerminalInstalled()) {
			if(!world.isRemote)
				player.addChatMessage(new ChatComponentTranslation("ac.terminal.alrdy_installed"));
		} else {
			if(!world.isRemote) {
				if(!player.capabilities.isCreativeMode)
					stack.stackSize--;
				tData.install();
				startInstalling();
			}
		}
        return stack;
    }
	
	@RegNetworkCall(side = Side.CLIENT)
	private static void startInstalling() {
		install();
	}
	
	@SideOnly(Side.CLIENT)
	private static void install() {
		AuxGuiHandler.register(new TerminalInstallEffect());
	}

}
