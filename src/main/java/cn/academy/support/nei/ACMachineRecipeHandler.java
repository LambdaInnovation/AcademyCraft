/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.support.nei;

import java.awt.Rectangle;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public abstract class ACMachineRecipeHandler extends TemplateRecipeHandler {
	
	@Override
	public abstract String getRecipeName();
	
	public abstract String getRecipeId();
	
	@Override
	public abstract String getGuiTexture();
	
	@Override
	public abstract Class<? extends GuiContainer> getGuiClass();
	
	public abstract List getRecipeList();
	
	public abstract int getInputX();
	public abstract int getInputY();
	public abstract int getOutputX();
	public abstract int getOutputY();
	public int getOtherX() {
		return 0;
	}
	public int getOtherY() {
		return 0;
	}
	public abstract Rectangle getTransferRectsRectangle();
	
	@Override
	public void loadTransferRects() {
		transferRects.add(new TemplateRecipeHandler.RecipeTransferRect(
				getTransferRectsRectangle(), getRecipeId()));
	}
	
	@Override
	public abstract void loadCraftingRecipes(String outputId, Object... results);
	
	@Override
	public abstract void loadCraftingRecipes(ItemStack result);
	
	@Override
	public abstract void loadUsageRecipes(ItemStack ingredient);
	
	public class ACCachedRecipe extends TemplateRecipeHandler.CachedRecipe {
		
		private final PositionedStack ingredient;
		private final PositionedStack result;
		private final PositionedStack otherStack;
		
		public ACCachedRecipe(ItemStack input, ItemStack output) {
			ingredient = new PositionedStack(input, getInputX(), getInputY());
			result = new PositionedStack(output, getOutputX(), getOutputY());
			otherStack = null;
		}
		
		public ACCachedRecipe(ItemStack input, ItemStack output, ItemStack other) {
			ingredient = new PositionedStack(input, getInputX(), getInputY());
			result = new PositionedStack(output, getOutputX(), getOutputY());
			otherStack = new PositionedStack(other, getOtherX(), getOtherY());
		}
		
		@Override
		public PositionedStack getIngredient() {
			return ingredient;
		}

		@Override
		public PositionedStack getResult() {
			return result;
		}
		
		@Override
		public PositionedStack getOtherStack() {
			return otherStack;
		}
		
	}

}
