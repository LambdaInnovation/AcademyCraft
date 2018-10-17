package cn.academy.support.minetweaker;

import cn.academy.crafting.ImagFusorRecipes;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 
 * @author 3TUSK
 */
@ZenClass("mods.academycraft.ImagFusor")
@ZenRegister
public class ImagFusorSupport {

    @ZenMethod
    public static void addRecipe(IItemStack output, IItemStack input, int liquidAmount) {
        CraftTweakerAPI.apply(new AddImagFusorRecipe(input, output, liquidAmount));
    }
    
    private static class AddImagFusorRecipe implements IAction
    {
        ItemStack input, output;
        int liquidAmount;
        
        public AddImagFusorRecipe(IItemStack input, IItemStack output, int liquidAmount) {
            this.input = MTSupport.toStack(input);
            this.output = MTSupport.toStack(output);
            this.liquidAmount = liquidAmount;
        }

        @Override
        public void apply() {
            ImagFusorRecipes.INSTANCE.addRecipe(input, liquidAmount, output);
        }

        @Override
        public String describe() {
            return "Add extra ImagFusor recipe for " + input.getTranslationKey();
        }
        
    }
}