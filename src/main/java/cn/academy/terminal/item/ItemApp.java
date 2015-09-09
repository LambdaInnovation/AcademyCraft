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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.item.ACItem;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ItemInstaller app
 * @author WeAthFolD
 */
public class ItemApp extends ACItem {
	
	static Map<App, ItemApp> items = new HashMap();
	
	public static void registerItems() {
		for(App app : AppRegistry.enumeration()) {
			if(!app.isPreInstalled()) {
				ItemApp item = new ItemApp(app);
				GameRegistry.registerItem(item, "ac_app_" + app.getName());
				AcademyCraft.recipes.map("app_" + app.getName(), item);
				items.put(app, item);
			}
		}
	}
	
	public static ItemApp getItemForApp(App app) {
		return items.get(app);
	}
	
	public final App app;

	private ItemApp(App _app) {
		super("apps");
		app = _app;
		setTextureName("academy:app_" + app.getName());
		this.setHasSubtypes(true);
	}
	
	@Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(!world.isRemote) {
			TerminalData terminalData = TerminalData.get(player);
			if(!terminalData.isTerminalInstalled()) {
				player.addChatMessage(new ChatComponentTranslation("ac.terminal.notinstalled"));
			} else if(terminalData.isInstalled(app)) {
				player.addChatMessage(new ChatComponentTranslation("ac.terminal.app_alrdy_installed", app.getDisplayName()));
			} else {
				if(!player.capabilities.isCreativeMode)
					stack.stackSize--;
				terminalData.installApp(app);
				player.addChatMessage(new ChatComponentTranslation("ac.terminal.app_installed", app.getDisplayName()));
			}
		}
        return stack;
    }
    
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
    	list.add(app.getDisplayName());
    }
    
}
