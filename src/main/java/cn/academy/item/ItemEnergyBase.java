package cn.academy.item;

import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author WeAthFolD
 *
 */
public class ItemEnergyBase extends Item implements ImagEnergyItem {
    
    protected static IFItemManager itemManager = IFItemManager.instance;
    
    public final double maxEnergy;
    public final double bandwidth;
    
//    IIcon iconEmpty, iconHalf, iconFull;
//    public boolean useMultipleIcon = true;
    
    public ItemEnergyBase(double _maxEnergy, double _bandwidth) {
        maxEnergy = _maxEnergy;
        bandwidth = _bandwidth;
        
        setMaxStackSize(1);
        setMaxDamage(13);
        addPropertyOverride(
            new ResourceLocation("energy"),
            (stack, worldIn, entityIn) -> {
                int damage = stack.getItemDamage();
                if (damage < 3)
                    return 1.0f;
                if (damage > 10)
                    return 0.0f;
                return 0.5f;
            }
        );
    }
    
//    @Override
//    @SideOnly(Side.CLIENT)
//    public void registerIcons(IIconRegister ir) {
//        if(useMultipleIcon) {
//            iconEmpty = ir.registerIcon("academy:" + name + "_empty");
//            iconHalf = ir.registerIcon("academy:" + name + "_half");
//            iconFull = ir.registerIcon("academy:" + name + "_full");
//        } else {
//            super.registerIcons(ir);
//        }
//    }
//
//    @Override
//    @SideOnly(Side.CLIENT)
//    public IIcon getIconFromDamage(int damage) {
//        if(!useMultipleIcon)
//            return super.getIconFromDamage(damage);
//
//        if(damage < 3) {
//            return iconFull;
//        }
//        if(damage > 10) {
//            return iconEmpty;
//        }
//        return iconHalf;
//    }
    
    @Override
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }
    
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (isInCreativeTab(tab)) {
            ItemStack is = new ItemStack(this);
            items.add(is);
            itemManager.charge(is, 0, true);

            is = new ItemStack(this);
            itemManager.charge(is, Double.MAX_VALUE, true);
            items.add(is);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(itemManager.getDescription(stack));
    }

}