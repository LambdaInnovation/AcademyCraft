package cn.academy.item;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import com.google.common.base.Preconditions;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import javax.annotation.Nullable;
import java.util.List;

public class ItemInductionFactor extends Item {

    public static Category getCategory(ItemStack stack) {
        checkStack(stack);
        return CategoryManager.INSTANCE.getCategory(stack.getItemDamage());
    }

    public static void setCategory(ItemStack stack, Category cat) {
        checkStack(stack);
        stack.setItemDamage(cat.getCategoryID());
    }

    private static void checkStack(ItemStack stack) {
        Preconditions.checkArgument(stack.getItem() instanceof ItemInductionFactor);
    }

    private IIcon[] icons;

    public ItemInductionFactor() {
        setMaxStackSize(1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister register) {
        icons = new IIcon[CategoryManager.INSTANCE.getCategoryCount()];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon("academy:factor_" +
                    CategoryManager.INSTANCE.getCategory(i).getName());
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        return icons[meta];
    }

    public ItemStack create(Category cat) {
        ItemStack stack = new ItemStack(this);
        setCategory(stack, cat);
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (Category c : CategoryManager.INSTANCE.getCategories()) {
            items.add(create(c));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(getCategory(stack).getDisplayName());
    }

}