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

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cn.academy.core.item.ACItem;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ItemInstaller app
 * @author WeAthFolD
 */
public class ItemApp extends ACItem {
	
	IIcon[] itemIcons;

	public ItemApp() {
		super("apps");
		this.setHasSubtypes(true);
	}
	
	@Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(!world.isRemote) {
			App app = getApp(stack);
			if(app != null) {
				TerminalData terminalData = TerminalData.get(player);
				if(terminalData.isInstalled(app)) {
					player.addChatMessage(new ChatComponentTranslation("ac.terminal.app_alrdy_installed", app.getDisplayName()));
				} else {
					if(!player.capabilities.isCreativeMode)
						stack.stackSize--;
					terminalData.installApp(app);
					player.addChatMessage(new ChatComponentTranslation("ac.terminal.app_installed", app.getDisplayName()));
				}
			}
		}
        return stack;
    }
	
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister ir) {
    	itemIcons = new IIcon[AppRegistry.enumeration().size()];
    	for(App app : AppRegistry.enumeration()) {
    		itemIcons[app.getID()] = ir.registerIcon("academy:app_" + app.getName());
    	}
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int damage) {
    	if(damage >= itemIcons.length)
    		damage = 0;
        return itemIcons[damage];
    }
	
	public App getApp(ItemStack stack) {
		return AppRegistry.INSTANCE.get(stack.getItemDamage());
	}
	
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs cct, List list) {
        for(App app : AppRegistry.enumeration()) {
        	list.add(new ItemStack(this, 1, app.getID()));
        }
    }
    
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
    	list.add(getApp(stack).getDisplayName());
    }
}
