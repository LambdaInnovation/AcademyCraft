package cn.academy.terminal.item;

import cn.academy.core.item.ACItem;
import cn.academy.terminal.TerminalData;
import cn.academy.terminal.client.TerminalInstallEffect;
import cn.academy.terminal.client.TerminalInstallerRenderer;
import cn.lambdalib2.annoreg.mc.RegItem;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib2.util.client.auxgui.AuxGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
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
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(ItemTerminalInstaller.class), "install");
            }
        }
        return stack;
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel="install", side=Side.CLIENT)
    private static void install() {
        AuxGuiHandler.register(new TerminalInstallEffect());
    }

}