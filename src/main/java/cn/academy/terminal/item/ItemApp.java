/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.item;

import cn.academy.core.AcademyCraft;
import cn.academy.core.item.ACItem;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ItemInstaller app
 * @author WeAthFolD
 */
@Registrant
public class ItemApp extends ACItem {
    
    static Map<App, ItemApp> items = new HashMap<>();

    @RegInitCallback
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
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
        list.add(app.getDisplayName());
    }
    
}
