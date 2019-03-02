package cn.academy.support.jei;

import cn.academy.ACBlocks;
import cn.academy.crafting.MetalFormerRecipes;
import cn.academy.crafting.MetalFormerRecipes.RecipeObject;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.recipe.IRecipeWrapper;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author KSkun
 *
 */
public class MetalFormerRecipeCategory extends IACRecipeCategory
{
    public static List<IRecipeWrapper> recipeWrapper = loadCraftingRecipes();
    private static ResourceLocation bg = new ResourceLocation("academy", "textures/guis/nei_metalformer.png");
    private IGuiHelper guiHelper;

    public MetalFormerRecipeCategory(IGuiHelper guiHelper)
    {
        super(ACBlocks.metal_former);
        this.guiHelper = guiHelper;
    }


    //TODO 物品槽的具体位置还需要调整
    @Override
    public List<SlotPos> getInputs() {
        return Collections.singletonList(new SlotPos(5, 23));
    }


    @Override
    public List<SlotPos> getOutputs() {
        return Collections.singletonList(new SlotPos(71, 23));
    }

    @Override
    public String getTitle() {
        return I18n.format("tile.ac_metal_former.name");
    }

    private static List<IRecipeWrapper> loadCraftingRecipes() {

        List<IRecipeWrapper> lists = new ArrayList<>();
        for(RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
            lists.add(iIngredients -> {
                iIngredients.setInput(ItemStack.class, r.input);
                iIngredients.setOutput(ItemStack.class, r.output);
                //r.mode
            });
        }
        return lists;
    }

    @Override
    public IDrawable getBackground() {
        IDrawable gui = guiHelper.createDrawable(bg,  0, 0, 94, 57, 94, 57);

        /*
        HudUtils.rect(35, 0, 0, 0, 94, 57, 94, 57);
        GL20.glUseProgram(0);
        if(tick >= 50) tick = 0;
        ShaderMono.instance().useProgram();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
        RenderUtils.loadTexture(((MFCachedRecipe) arecipes.get(recipe)).mode.texture);
        HudUtils.rect(73, 23, 0, 0, 18, 18, 48, 48);
        RenderUtils.loadTexture(new ResourceLocation("academy:textures/guis/progress/progress_metalformer.png"));
        HudUtils.rect(57, 41, 0, 0, 50d * (tick / 50d), 13, 114d * (tick / 50d), 30);
        GL20.glUseProgram(0);
         */
        return gui;
    }


}