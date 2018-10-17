package cn.academy.support.minetweaker;

import cn.academy.block.tileentity.TileMetalFormer.Mode;
import cn.academy.crafting.MetalFormerRecipes;
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
@ZenClass("mods.academycraft.MetalFormer")
@ZenRegister
public class MetalFormerSupport {
    
    @ZenMethod
    public static void addEtchRecipe(IItemStack output, IItemStack input) {
        CraftTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.ETCH));
    }
    
    @ZenMethod
    public static void addInciseRecipe(IItemStack output, IItemStack input) {
        CraftTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.INCISE));
    }
    
    @ZenMethod
    public static void addPlateRecipe(IItemStack output, IItemStack input) {
        CraftTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.PLATE));
    }
    
    private static class AddMetalFormerRecipe implements IAction
    {

        ItemStack input, output;
        Mode mode;
        
        public AddMetalFormerRecipe(IItemStack input, IItemStack output, Mode mode) {
            this.input = MTSupport.toStack(input);
            this.output = MTSupport.toStack(output);
            this.mode = mode;
        }
        
        @Override
        public void apply() {
            MetalFormerRecipes.INSTANCE.add(input, output, mode);
        }

        @Override
        public String describe() {
            return "Add extra metal former etching recipe for " + input.getTranslationKey();
        }
        
    }

}