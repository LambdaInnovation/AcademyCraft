package cn.academy.misc.tutorial;

import cn.academy.core.item.ACItem;
import cn.academy.misc.tutorial.client.GuiTutorial;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class ItemTutorial extends ACItem {

    public ItemTutorial() {
        super("tutorial");
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if(world.isRemote) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
        }
        return stack;
    }
    
}