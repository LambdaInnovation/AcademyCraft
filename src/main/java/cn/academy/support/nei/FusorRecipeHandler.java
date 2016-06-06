/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.support.nei;

import cn.academy.core.Resources;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.client.shader.ShaderMono;
import cn.lambdalib.util.helper.Color;
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
        return GuiContainer.class;
    }

    @Override
    public int getInputX() {
        return 30;
    }

    @Override
    public int getInputY() {
        return 44;
    }

    @Override
    public int getOutputX() {
        return 118;
    }

    @Override
    public int getOutputY() {
        return 44;
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for(IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
            if(r.output.getItem().equals(result.getItem())) {
                arecipes.add(new IFCachedRecipe(r.consumeType, r.output, r.consumeLiquid));
                break;
            }    
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for(IFRecipe r : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
            if(r.consumeType.getItem().equals(ingredient.getItem())) {
                arecipes.add(new IFCachedRecipe(r.consumeType, r.output, r.consumeLiquid));
                break;
            }    
        }
    }
    
    @Override
    public void drawExtras(int recipe) {
        if(tick >= 50) tick = 0;
        Resources.font().draw(String.valueOf(((IFCachedRecipe) arecipes.get(recipe)).liquid),
                75, 7, new FontOption(13, new Color(3618615)));
        ShaderMono.instance().useProgram();
        GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
        RenderUtils.loadTexture(new ResourceLocation("academy:textures/guis/progress/progress_fuser.png"));
        HudUtils.rect(56, 45, 0, 0, 51d * (tick / 50d), 15, 64d * (tick / 50d), 16);
        GL20.glUseProgram(0);
    }
    
    private class IFCachedRecipe extends ACCachedRecipe {
        
        public final int liquid;

        private IFCachedRecipe(ItemStack input, ItemStack output, int liquid) {
            super(input, output);
            this.liquid = liquid;
        }
        
    }

}
