package cn.academy.ability.item;

import cn.academy.ability.client.render.RenderDeveloperPortable;
import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.PortableDevData;
import cn.academy.energy.template.ItemEnergyBase;
import cn.lambdalib2.annoreg.mc.RegItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class ItemDeveloper extends ItemEnergyBase {
    
    @SideOnly(Side.CLIENT)
    @RegItem.Render
    public static RenderDeveloperPortable renderer;
    
    public static final DeveloperType type = DeveloperType.PORTABLE;

    public ItemDeveloper() {
        super("developer_portable", type.getEnergy(), type.getBandwidth());
        this.bFull3D = true;
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if(player.worldObj.isRemote) {
            displayGui(player);
        }
        
        return stack;
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