package cn.academy.energy.item;

import cn.academy.core.item.ACItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class ItemMatrixCore extends ACItem {
    
    int LEVELS = 3;
    
    IIcon icons[] = new IIcon[LEVELS];

    public ItemMatrixCore() {
        super("matrix_core");
        this.setHasSubtypes(true);
    }
    
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + "_" + stack.getItemDamage();
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister ir) {
        for(int i = 0; i < LEVELS; ++i) {
            icons[i] = ir.registerIcon("academy:matrix_core_" + i);
        }
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        return icons[meta];
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs cct, List list) {
        for(int i = 0; i < LEVELS; ++i)
            list.add(new ItemStack(this, 1, i));
    }

}