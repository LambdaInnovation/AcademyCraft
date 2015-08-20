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

import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.academy.crafting.client.gui.GuiImagFusor;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class FusorRecipeHandler extends ACMachineRecipeHandler {

	@Override
	public String getRecipeName() {
		return StatCollector.translateToLocal("tile.ac_imag_fusor.name");
	}

	@Override
	public String getGuiTexture() {
		return "academy:textures/guis/window_imagFuser.png";
	}
	
	@Override
    public Class<? extends GuiContainer> getGuiClass() {
        return GuiImagFusor.class;
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
	public int getOtherX() {
		return 0;
	}
	
	@Override
	public int getOtherY() {
		return 0;
	}

	@Override
	public String getRecipeId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getRecipeList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Rectangle getTransferRectsRectangle() {
		// TODO Auto-generated method stub
		return null;
	}

}
