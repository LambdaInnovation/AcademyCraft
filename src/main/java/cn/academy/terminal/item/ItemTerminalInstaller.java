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

import cn.academy.core.item.ACItem;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.client.TerminalInstallEffect;
import cn.academy.terminal.client.TerminalInstallerRenderer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Target;
import cn.lambdalib.util.client.auxgui.AuxGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

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
				startInstalling(player);
			}
		}
        return stack;
    }
	
	@RegNetworkCall(side = Side.CLIENT)
	private static void startInstalling(@Target EntityPlayer player) {
		install();
	}
	
	@SideOnly(Side.CLIENT)
	private static void install() {
		AuxGuiHandler.register(new TerminalInstallEffect());
	}

}
