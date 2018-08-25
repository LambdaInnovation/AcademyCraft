package cn.academy.support.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IRecipeWrapper;

import java.util.Collection;

@JEIPlugin
public class SubModule implements IModPlugin {
    public FusorRecipeCategory fusorRecipeCategory;

    public SubModule() {
    }

    @Override
    public void register(IModRegistry registry)
    {
        addMachineRecipes(registry, FusorRecipeCategory.recipeWrapper, fusorRecipeCategory);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry){
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        fusorRecipeCategory = new FusorRecipeCategory(guiHelper);
        registry.addRecipeCategories(fusorRecipeCategory);
    }

    private void addMachineRecipes(IModRegistry registry, Collection<IRecipeWrapper> wrappers, FusorRecipeCategory category) {
        registry.addRecipes(wrappers, category.getUid());
        registry.addRecipeCatalyst(category.getBlockStack(), category.getUid());
    }

}
