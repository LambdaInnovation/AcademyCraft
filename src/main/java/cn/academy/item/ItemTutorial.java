package cn.academy.item;

import cn.academy.advancements.ACAchievements;
import cn.academy.client.gui.GuiTutorial;
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
public class ItemTutorial extends Item {

    @SideOnly(Side.CLIENT)
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
        }
        ACAchievements.trigger(player, ACAchievements.aOpenTutorial.ID);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
}