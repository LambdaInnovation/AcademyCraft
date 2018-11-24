package cn.academy.item;

import cn.academy.AcademyCraft;
import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cn.lambdalib2.registry.RegistryCallback;
import cn.lambdalib2.util.SideUtils;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ItemInstaller app
 * TODO: Automate json stuff for this item?
 * @author WeAthFolD
 */
public class ItemApp extends Item {
    
    private static Map<App, ItemApp> items = new HashMap<>();

    @RegistryCallback
    @SuppressWarnings("sideonly")
    private static void regItem(RegistryEvent.Register<Item> event) {
        for(App app : AppRegistry.enumeration()) {
            if(!app.isPreInstalled()) {
                ItemApp item = new ItemApp(app);
                item.setTranslationKey("ac_apps");
                item.setRegistryName("academy:app_" + app.getName());

                event.getRegistry().register(item);
                AcademyCraft.recipes.map("app_" + app.getName(), item);
                items.put(app, item);
            }
        }

        if (SideUtils.isClient())
            registerItemModels();
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModels() {
        for (App app : AppRegistry.enumeration()) {
            if (!app.isPreInstalled()) {
                ItemApp item = items.get(app);
                ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
    }
    
    public static ItemApp getItemForApp(App app) {
        return items.get(app);
    }
    
    public final App app;

    private ItemApp(App _app) {
        app = _app;
        setCreativeTab(AcademyCraft.cct);
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(!world.isRemote) {
            TerminalData terminalData = TerminalData.get(player);
            if(!terminalData.isTerminalInstalled()) {
                player.sendMessage(new TextComponentTranslation("ac.terminal.notinstalled"));
            } else if(terminalData.isInstalled(app)) {
                player.sendMessage(new TextComponentTranslation("ac.terminal.app_alrdy_installed", app.getDisplayName()));
            } else {
                if(!player.capabilities.isCreativeMode)
                    stack.setCount(stack.getCount()-1);
                terminalData.installApp(app);
                player.sendMessage(new TextComponentTranslation("ac.terminal.app_installed", app.getDisplayName()));
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(app.getDisplayName());
    }
    
}