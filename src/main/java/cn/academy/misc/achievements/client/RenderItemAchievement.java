package cn.academy.misc.achievements.client;

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
		HudUtils.rect(16, 16);
	}

}
