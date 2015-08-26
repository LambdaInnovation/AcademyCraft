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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cn.academy.core.client.Resources;
import cn.academy.core.client.glsl.ShaderMono;
import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.api.MetalFormerRecipes.RecipeObject;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.academy.crafting.client.gui.GuiMetalFormer;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Font;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
	public String getGuiTexture() {
		return "academy:textures/guis/nei_metalformer.png";
	}

	@Override
	public Class<? extends GuiContainer> getGuiClass() {
		return GuiMetalFormer.class;
	}

	@Override
	public int getInputX() {
		return 42;
	}

	@Override
	public int getInputY() {
		return 38;
	}

	@Override
	public int getOutputX() {
		return 108;
	}

	@Override
	public int getOutputY() {
		return 38;
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		for(RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
			if(r.output.getItem().equals(result.getItem())) {
				arecipes.add(new MFCachedRecipe(r.input, r.output, r.mode));
			}
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		for(RecipeObject r : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
			if(r.input.getItem().equals(ingredient.getItem())) {
				arecipes.add(new MFCachedRecipe(r.input, r.output, r.mode));
			}
		}
	}
	
	@Override
	public void drawExtras(int recipe) {
		if(tick >= 50) tick = 0;
		ShaderMono.instance().useProgram();
		GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
		RenderUtils.loadTexture(((MFCachedRecipe) arecipes.get(recipe)).mode.texture);
		HudUtils.rect(76, 14, 0, 0, 14, 14, 24, 24);
		new ResourceLocation("academy:textures/guis/progress/progress_former.png");
		HudUtils.rect(68, 43, 0, 0, 30d * (tick / 50d), 6, 30d * (tick / 50d), 6);
		GL20.glUseProgram(0);
	}
	
	private class MFCachedRecipe extends ACCachedRecipe {
		
		public final Mode mode;

		public MFCachedRecipe(ItemStack input, ItemStack output, Mode mode) {
			super(input, output);
			this.mode = mode;
		}
		
	}

}
