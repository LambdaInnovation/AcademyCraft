package cn.academy.item;

import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.PortableDevData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @author WeAthFolD
 */
public class ItemDeveloper extends ItemEnergyBase {
    
//    @SideOnly(Side.CLIENT)
//    @RegItem.Render
//    public static RenderDeveloperPortable renderer;
//
    public static final DeveloperType type = DeveloperType.PORTABLE;

    public ItemDeveloper() {
        super(type.getEnergy(), type.getBandwidth());
        this.bFull3D = true;

    }
    
    @Override
    @SuppressWarnings("sideonly")
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if(player.world.isRemote) {
            displayGui(player);
        }
        
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
    
    @SideOnly(Side.CLIENT)
    private void displayGui(EntityPlayer player) {
        Minecraft.getMinecraft().displayGuiScreen(DeveloperUI.apply(PortableDevData.get(player)));
    }

    @Override
    public double getMaxEnergy() {
        return type.getEnergy();
    }

    @Override
    public double getBandwidth() {
        return type.getBandwidth();
    }

}