package cn.academy.support.jei;

import cn.academy.ACBlocks;
import cn.academy.AcademyCraft;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.lambdalib2.util.RenderUtils;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.recipe.IRecipeWrapper;
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
public class FusorRecipeCategory extends IACRecipeCategory
{
    public static List<IRecipeWrapper> recipeWrapper = loadCraftingRecipes();
    private static ResourceLocation bg = new ResourceLocation("academy", "textures/guis/nei_fusor.png");
    private IGuiHelper guiHelper;

    public FusorRecipeCategory(IGuiHelper guiHelper)
    {
        super(ACBlocks.imag_fusor);
        this.guiHelper = guiHelper;
    }

    @Override
    public String getTitle() {
        return I18n.format("tile.ac_imag_fusor.name");
    }

    //TODO 物品槽的具体位置还需要调整
    @Override
    public List<SlotPos> getInputs() {
        return Collections.singletonList(new SlotPos(5, 36));
    }


    @Override
    public List<SlotPos> getOutputs() {
        return Collections.singletonList(new SlotPos(93, 36));
    }

    private static List<IRecipeWrapper> loadCraftingRecipes() {
        List<IRecipeWrapper> lists = new ArrayList<>();
        for(IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
            lists.add(iIngredients -> {
                iIngredients.setInput(ItemStack.class, r.consumeType);
                iIngredients.setOutput(ItemStack.class, r.output);
                //r.cost
            });
        }
        return lists;
    }

    @Override
    public IDrawable getBackground() {
        IDrawable gui = guiHelper.createDrawable(bg,  0, 0, 115, 66, 115, 66);
        /*

        HudUtils.rect(24.5f, 0, 0, 0, 115, 66, 115, 66);
        GL20.glUseProgram(0);
        if(tick >= 50) tick = 0;
        GL11.glEnable(GL11.GL_BLEND);
        Resources.font().draw(String.valueOf(((IFCachedRecipe) arecipes.get(recipe)).liquid),
                75, 11.5f, new FontOption(13, new Color(0xFF373737)));
        ShaderMono.instance().useProgram();
        GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
        RenderUtils.loadTexture(new ResourceLocation("academy:textures/guis/progress/progress_fusor.png"));
        HudUtils.rect(62, 40.5f, 0, 0, 40d * (tick / 50d), 10, 126 * (tick / 50d), 30);
        GL20.glUseProgram(0);
         */
        return gui;
    }

}