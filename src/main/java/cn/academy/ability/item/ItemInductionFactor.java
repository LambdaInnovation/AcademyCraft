package cn.academy.ability.item;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.core.item.ACItem;
import com.google.common.base.Preconditions;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemInductionFactor extends ACItem {

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

    public ItemInductionFactor() {
        super("induction_factor");
        setMaxStackSize(1);
    }

    public ItemStack create(Category cat) {
        ItemStack stack = new ItemStack(this);
        setCategory(stack, cat);
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (Category c : CategoryManager.INSTANCE.getCategories()) {
            list.add(create(c));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
        list.add(getCategory(stack).getDisplayName());
    }

}
