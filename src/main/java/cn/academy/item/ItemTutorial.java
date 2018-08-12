package cn.academy.item;

import cn.academy.client.gui.GuiTutorial;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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