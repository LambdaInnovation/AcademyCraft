package cn.academy.support.nei;

import cn.academy.core.Resources;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.ImagFusorRecipes.IFRecipe;
import cn.lambdalib2.util.client.HudUtils;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.client.font.IFont.FontOption;
import cn.lambdalib2.util.client.shader.ShaderMono;
import cn.lambdalib2.util.helper.Color;
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
        return 31;
    }

    @Override
    public int getInputY() {
        return 37;
    }

    @Override
    public int getOutputX() {
        return 118;
    }

    @Override
    public int getOutputY() {
        return 37;
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
    public void drawBackground(int recipe) {
        ShaderMono.instance().useProgram();
        RenderUtils.loadTexture(new ResourceLocation(getGuiTexture()));
        HudUtils.rect(24.5f, 0, 0, 0, 115, 66, 115, 66);
        GL20.glUseProgram(0);
    }
    
    @Override
    public void drawExtras(int recipe) {
        if(tick >= 50) tick = 0;
        GL11.glEnable(GL11.GL_BLEND);
        Resources.font().draw(String.valueOf(((IFCachedRecipe) arecipes.get(recipe)).liquid),
                75, 11.5f, new FontOption(13, new Color(0xFF373737)));
        ShaderMono.instance().useProgram();
        GL11.glColor4f(55f / 151, 55f / 151, 55f / 151, 1);
        RenderUtils.loadTexture(new ResourceLocation("academy:textures/guis/progress/progress_fusor.png"));
        HudUtils.rect(62, 40.5f, 0, 0, 40d * (tick / 50d), 10, 126 * (tick / 50d), 30);
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