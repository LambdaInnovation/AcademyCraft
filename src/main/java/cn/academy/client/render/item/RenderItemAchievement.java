package cn.academy.client.render.item;

import cn.academy.item.ItemAchievement;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/**
 * TODO
 * @author EAirPeter
 */
//@SideOnly(Side.CLIENT)
//public class RenderItemAchievement implements IItemRenderer {
//
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return type == ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//            ItemRendererHelper helper) {
//        return false;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        RenderUtils.loadTexture(ItemAchievement.getTexture(item.getItemDamage()));
//        GL11.glEnable(GL11.GL_BLEND);
//        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        HudUtils.rect(16, 16);
//        GL11.glDisable(GL11.GL_BLEND);
//    }
//
//}