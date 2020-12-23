package cn.academy.item;

import cn.academy.advancements.ACAdvancements;
import cn.academy.terminal.TerminalData;
import cn.academy.client.auxgui.TerminalInstallEffect;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class ItemTerminalInstaller extends Item  {

    public ItemTerminalInstaller() {
        this.bFull3D = true;
        this.maxStackSize = 1;
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        TerminalData tData = TerminalData.get(player);
        if(tData.isTerminalInstalled()) {
            if(!world.isRemote)
                player.sendMessage(new TextComponentTranslation("ac.terminal.alrdy_installed"));
        } else {
            if(!world.isRemote) {
                if(!player.capabilities.isCreativeMode)
                    stack.setCount(stack.getCount() - 1);
                tData.install();
                ACAdvancements.trigger(player, ACAdvancements.terminal_installed.ID);
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(ItemTerminalInstaller.class), "install");
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel="install", side=Side.CLIENT)
    private static void install() {
        AuxGuiHandler.register(new TerminalInstallEffect());
    }

}