/**
 * 
 */
package cn.academy.misc.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.register.ACItems;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class RendererCoin extends Render {

	public RendererCoin() {}

	@Override
	public void doRender(Entity var1, double x, double y, double z,
			float var8, float var9) {
		EntityThrowingCoin etc = (EntityThrowingCoin) var1;
		EntityPlayer player = etc.player;
		boolean fp = player == Minecraft.getMinecraft().thePlayer 
				&& Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		double dt = Minecraft.getSystemTime() - GenericUtils.loadCompound(etc.stack).getLong("startTime");
		GL11.glPushMatrix(); {
			GL11.glTranslated(x, y, z);
			if(fp) {
				GL11.glRotated(player.rotationYaw, 0, -1, 0);
			} else GL11.glRotated(player.renderYawOffset, 0, -1, 0);
			GL11.glTranslated(-0.63, -0.60, 0.30);
			float scale = 0.3F;
			GL11.glScalef(scale, scale, scale);
			GL11.glTranslated(0.5, 0.5, 0);
			GL11.glRotated(dt * 2, etc.axis.xCoord, etc.axis.yCoord, etc.axis.zCoord);
			GL11.glTranslated(-0.5, -0.5, 0);
			RenderUtils.renderItemIn2d(0.0625, ACClientProps.TEX_COIN_FRONT, ACClientProps.TEX_COIN_BACK);
		} GL11.glPopMatrix();
	}
	
	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}
	
	public static class ItemRender implements IItemRenderer {
		@Override
		public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
			return type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.EQUIPPED;
		}
	
		@Override
		public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
				ItemRendererHelper helper) {
			return false;
		}
	
		@Override
		public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
			final float sc = 0.7F;
			if(!ACItems.coin.inProgress(item)) {
				GL11.glScalef(sc, sc, sc);
				RenderUtils.renderItemIn2d(0.0625, ACClientProps.TEX_COIN_FRONT, ACClientProps.TEX_COIN_BACK);
				return;
			}
		}
	}

}
