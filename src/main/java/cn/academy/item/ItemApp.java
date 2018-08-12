package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
public class ItemApp extends ACItem {
    
    static Map<App, ItemApp> items = new HashMap<>();

    @RegInitCallback
    private static void init() {
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