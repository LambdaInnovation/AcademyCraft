/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.nei;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author KSkun
 *
 */
public abstract class ACMachineRecipeHandler extends TemplateRecipeHandler {
    
    protected int tick;
    
    @Override
    public abstract String getRecipeName();
    
    @Override
    public abstract String getGuiTexture();
    @Override
    public abstract Class<? extends GuiContainer> getGuiClass();
    
    public abstract int getInputX();
    public abstract int getInputY();
    public abstract int getOutputX();
    public abstract int getOutputY();
    
    @Override
    public abstract void loadCraftingRecipes(ItemStack result);
    @Override
    public abstract void loadUsageRecipes(ItemStack ingredient);
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        ++tick;
    }
    
    protected class ACCachedRecipe extends CachedRecipe {
        
        private final PositionedStack ingredient;
        private final PositionedStack result;
        
        protected ACCachedRecipe(ItemStack input, ItemStack output) {
            ingredient = new PositionedStack(input, getInputX(), getInputY());
            result = new PositionedStack(output, getOutputX(), getOutputY());
        }
        
        @Override
        public PositionedStack getIngredient() {
            return ingredient;
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }
        
    }

}
