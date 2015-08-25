package cn.academy.misc.achievements.client;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.misc.achievements.ItemAchievement;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

/**
 * @author EAirPeter
 */
@SideOnly(Side.CLIENT)
public class RenderItemAchievement implements IItemRenderer {

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return type == ItemRenderType.INVENTORY;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		return false;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		RenderUtils.loadTexture(ItemAchievement.getTexture(item.getItemDamage()));
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		HudUtils.rect(16, 16);
		GL11.glDisable(GL11.GL_BLEND);
	}

}
