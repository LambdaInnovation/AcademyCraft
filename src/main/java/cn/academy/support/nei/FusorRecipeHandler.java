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

import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.client.gui.GuiImagFusor;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import codechicken.nei.recipe.TemplateRecipeHandler;

/**
 * 
 * @author KSkun
 *
 */
public class FusorRecipeHandler extends ACMachineRecipeHandler {

	@Override
	public String getRecipeName() {
		return StatCollector.translateToLocal("tile.ac_imag_fusor.name");
	}

	@Override
	public String getGuiTexture() {
		return "academy:textures/guis/nei_fusor.png";
	}
	
	@Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiImagFusor.class;
    }

	@Override
	public int getInputX() {
		return 32;
	}

	@Override
	public int getInputY() {
		return 44;
	}

	@Override
	public int getOutputX() {
		return 120;
	}

	@Override
	public int getOutputY() {
		return 44;
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		for(IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
			if(r.output.getItem().equals(result.getItem())) {
				arecipes.add(new ACCachedRecipe(r.consumeType, r.output));
				break;
			}	
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		for(IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
			if(r.consumeType.getItem().equals(ingredient.getItem())) {
				arecipes.add(new ACCachedRecipe(r.consumeType, r.output));
				break;
			}	
		}
	}

	@Override
	public Rectangle getTransferRectsRectangle() {
		return new Rectangle(0, 0, 0, 0);
	}

	@Override
	public String getMachineName() {
		return "fusor";
	}

}
