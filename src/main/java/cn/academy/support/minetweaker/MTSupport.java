package cn.academy.support.minetweaker;

import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author 3TUSK
 */
public final class MTSupport {

    static ItemStack toStack(IItemStack s) {
        return CraftTweakerMC.getItemStack(s);
    }

}