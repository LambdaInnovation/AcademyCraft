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

import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.api.MetalFormerRecipes.RecipeObject;
import cn.academy.crafting.client.gui.GuiMetalFormer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

/**
 * 
 * @author KSkun
 *
 */
public class MetalFormerRecipeHandler extends ACMachineRecipeHandler {

	@Override
	public String getRecipeName() {
		return StatCollector.translateToLocal("tile.ac_metal_former.name");
	}

	@Override
	public String getMachineName() {
		return "metal_former";
	}

	@Override
	public String getGuiTexture() {
		return "academy:textures/guis/window_metalformer.png";
	}

	@Override
	public Class<? extends GuiContainer> getGuiClass() {
		return GuiMetalFormer.class;
	}

	@Override
	public int getInputX() {
		return 0;
	}

	@Override
	public int getInputY() {
		return 0;
	}

	@Override
	public int getOutputX() {
		return 0;
	}

	@Override
	public int getOutputY() {
		return 0;
	}

	@Override
	public Rectangle getTransferRectsRectangle() {
		return new Rectangle(0, 0, 0, 0);
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		for(RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
			if(r.output.getItem().equals(result.getItem())) {
				arecipes.add(new ACCachedRecipe(r.input, r.output));
			}
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		for(RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
			if(r.input.getItem().equals(ingredient.getItem())) {
				arecipes.add(new ACCachedRecipe(r.input, r.output));
			}
		}
	}

}
