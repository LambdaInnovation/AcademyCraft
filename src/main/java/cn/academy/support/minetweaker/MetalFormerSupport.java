package cn.academy.support.minetweaker;

import cn.academy.block.tileentity.TileMetalFormer.Mode;
import minetweaker.MineTweakerAPI;
import minetweaker.OneWayAction;
import minetweaker.api.item.IItemStack;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import static cn.academy.crafting.MetalFormerRecipes.INSTANCE;
import static cn.academy.support.minetweaker.MTSupport.toStack;
/**
 * 
 * @author 3TUSK
 */
@ZenClass("mods.academycraft.MetalFormer")
public class MetalFormerSupport {
    
    @ZenMethod
    public static void addEtchRecipe(IItemStack output, IItemStack input) {
        MineTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.ETCH));
    }
    
    @ZenMethod
    public static void addInciseRecipe(IItemStack output, IItemStack input) {
        MineTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.INCISE));
    }
    
    @ZenMethod
    public static void addPlateRecipe(IItemStack output, IItemStack input) {
        MineTweakerAPI.apply(new AddMetalFormerRecipe(input, output, Mode.PLATE));
    }
    
    private static class AddMetalFormerRecipe extends OneWayAction {

        ItemStack input, output;
        Mode mode;
        
        public AddMetalFormerRecipe(IItemStack input, IItemStack output, Mode mode) {
            this.input = toStack(input);
            this.output = toStack(output);
            this.mode = mode;
        }
        
        @Override
        public void apply() {
            INSTANCE.add(input, output, mode);
        }

        @Override
        public String describe() {
            return "Add extra metal former etching recipe for " + input.getUnlocalizedName();
        }

        @Override
        public Object getOverrideKey() {
            return null;
        }
        
    }

}