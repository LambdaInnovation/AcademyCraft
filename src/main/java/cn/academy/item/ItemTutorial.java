package cn.academy.item;

import cn.academy.advancements.ACAdvancements;
import cn.academy.client.gui.GuiTutorial;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class ItemTutorial extends Item {

    private static final String MSG_TRIGGER = ".item.tutorial.trigger_advance";
    @SideOnly(Side.CLIENT)
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
        }
        NetworkMessage.sendToServer(this, MSG_TRIGGER, player);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Listener(channel=MSG_TRIGGER, side = Side.SERVER)
    public void onTrigger(EntityPlayer player)
    {
        ACAdvancements.trigger(player, ACAdvancements.open_misaka_cloud.ID);
    }
    
}