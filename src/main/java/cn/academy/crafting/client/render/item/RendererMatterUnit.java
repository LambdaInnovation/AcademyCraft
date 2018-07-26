package cn.academy.crafting.client.render.item;

import cn.academy.core.Resources;
import cn.academy.core.client.render.shader.ShaderMask;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.lambdalib2.util.client.RenderUtils;
import cn.lambdalib2.util.helper.GameTimer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 *
 */
public class RendererMatterUnit implements IItemRenderer {
    
    ResourceLocation texMask;
    
    public RendererMatterUnit() {
        texMask = Resources.getTexture("items/matter_unit/mask");
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
            ItemRendererHelper helper) {
        return false;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        ItemMatterUnit item = ModuleCrafting.matterUnit;
        GL11.glColor4d(1, 1, 1, 1);
        if(type != ItemRenderType.INVENTORY) {
            GL11.glPushMatrix(); {
                if(type == ItemRenderType.ENTITY)
                    GL11.glTranslated(-.5, -0.1, 0);
                
                RenderUtils.drawEquippedItem(stack, 0.0625f);
                
                GL11.glColorMask(false,false,false,false);
                RenderUtils.drawEquippedItem(0.0626f, texMask, texMask);
                GL11.glColorMask(true, true, true, true);
                
                GL11.glDepthFunc(GL11.GL_EQUAL);
                MatterMaterial mat = item.getMaterial(stack);
                RenderUtils.drawEquippedItemOverlay(0.0626f, mat.texture);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
            } GL11.glPopMatrix();
        } else {
            ShaderMask shader = ShaderMask.instance;
            float du = -(GameTimer.getAbsTime() % 10000L) / 1e4f, dv = (GameTimer.getAbsTime() % 10000L) / 1e4f;
            
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            RenderUtils.renderItemInventory(stack);
            
            shader.start(texMask);
            RenderUtils.loadTexture(item.getMaterial(stack).texture);
            
            GL11.glBegin(GL11.GL_QUADS);
            
            GL11.glTexCoord2f(0 + du, 0 + dv);
            shader.maskTexCoord(0, 0);
            GL11.glVertex2f(0, 0);
            
            GL11.glTexCoord2f(0 + du, 1 + dv);
            shader.maskTexCoord(0, 1);
            GL11.glVertex2f(0, 16);
            
            GL11.glTexCoord2f(1 + du, 1 + dv);
            shader.maskTexCoord(1, 1);
            GL11.glVertex2f(16, 16);
            
            GL11.glTexCoord2f(1 + du, 0 + dv);
            shader.maskTexCoord(1, 0);
            GL11.glVertex2f(16, 0);
            
            GL11.glEnd();
            
            shader.end();
            
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

}