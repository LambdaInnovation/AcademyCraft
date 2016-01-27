package cn.academy.misc.achievements;

import cn.academy.core.client.Resources;
import cn.academy.misc.achievements.client.RenderItemAchievement;
import cn.lambdalib.annoreg.mc.RegItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;

/**
 * @author EAirPeter
 */
public final class ItemAchievement extends Item {

    @RegItem.Render
    @SideOnly(Side.CLIENT)
    public static RenderItemAchievement render;
    
    private static ItemAchievement ITEM = null;
    
    private static final ArrayList<ResourceLocation> list;
    
    static {
        list = new ArrayList<ResourceLocation>();
        list.add(Resources.getTexture("null"));
    }
    
    public static ItemStack getStack(String texname) {
        return getStack(Resources.getTexture(texname));
    }
    
    public static ItemStack getStack(ResourceLocation tex) {
        int idx = list.indexOf(tex);
        if (idx == -1) {
            idx = list.size();
            list.add(tex);
        }
        return new ItemStack(ITEM, 1, idx);
    }
    
    public static ResourceLocation getTexture(int idx) {
        return list.get(idx);
    }
    
    public ItemAchievement() {
        if (ITEM == null)
            ITEM = this;
        else
            throw new IllegalStateException("Only one ItemAchievement is allowed");
    }
    
}
