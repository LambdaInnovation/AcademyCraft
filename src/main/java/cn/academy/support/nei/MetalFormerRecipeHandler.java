/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.nei;

import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.api.MetalFormerRecipes.RecipeObject;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import net.minecraft.client.gui.inventory.GuiContainer;
import cn.lambdalib2.util.client.HudUtils;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.client.shader.ShaderMono;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

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
        return GuiContainer.class;
    }

    @Override
    public int getInputX() {
        return 41;
    }

    @Override
    public int getInputY() {
        return 24;
    }

    @Override
    public int getOutputX() {
        return 107;
    }

    @Override
    public int getOutputY() {
        return 24;
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
    public void drawBackground(int recipe) {
        ShaderMono.instance().useProgram();
        RenderUtils.loadTexture(new ResourceLocation(getGuiTexture()));
        HudUtils.rect(35, 0, 0, 0, 94, 57, 94, 57);
        GL20.glUseProgram(0);
    }
    
    @Override
    public void drawExtras(int recipe) {
        if(tick >= 50) tick = 0;
        ShaderMono.instance().useProgram();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
        RenderUtils.loadTexture(((MFCachedRecipe) arecipes.get(recipe)).mode.texture);
        HudUtils.rect(73, 23, 0, 0, 18, 18, 48, 48);
        RenderUtils.loadTexture(new ResourceLocation("academy:textures/guis/progress/progress_metalformer.png"));
        HudUtils.rect(57, 41, 0, 0, 50d * (tick / 50d), 13, 114d * (tick / 50d), 30);
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
