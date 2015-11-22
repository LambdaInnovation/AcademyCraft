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
package cn.academy.vanilla.electromaster.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.GameTimer;

/**
 * 
 * @author KSkun
 */
public class RendererCoinThrowing extends Render {

	public RendererCoinThrowing() {}

	@Override
	public void doRender(Entity var1, double x, double y, double z,
			float var8, float var9) {
		EntityCoinThrowing etc = (EntityCoinThrowing) var1;
		EntityPlayer player = etc.player;
		boolean fp = player == Minecraft.getMinecraft().thePlayer 
				&& Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		
		double dt = GameTimer.getTime() % 150;
		
		if(etc.player == null)
			return;
		//If syncedSingle and in client computer, do not render
		if(etc.isSync && player == Minecraft.getMinecraft().thePlayer)
			return;
		if(etc.posY < player.posY)
			return;
		GL11.glPushMatrix(); {
			//x = player.posX - RenderManager.renderPosX;
			//y = etc.posY - RenderManager.renderPosY;
			//z = player.posZ - RenderManager.renderPosZ;
			if(player == Minecraft.getMinecraft().thePlayer) {
				x = z = 0;
			}
			
			GL11.glTranslated(x, y, z);
			if(fp) {
				GL11.glRotated(player.rotationYaw, 0, -1, 0);
			} else GL11.glRotated(player.renderYawOffset, 0, -1, 0);
			GL11.glTranslated(-0.63, -0.60, 0.30);
			float scale = 0.3F;
			GL11.glScalef(scale, scale, scale);
			GL11.glTranslated(0.5, 0.5, 0);
			GL11.glRotated((dt * 360.0 / 300.0), etc.axis.xCoord, etc.axis.yCoord, etc.axis.zCoord);
			GL11.glTranslated(-0.5, -0.5, 0);
			RenderUtils.drawEquippedItem(0.0625, Resources.TEX_COIN_FRONT, Resources.TEX_COIN_BACK);
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
			EntityLivingBase elb = (EntityLivingBase) data[1];
			if(!(elb instanceof EntityPlayer)) return;
			EntityPlayer player = (EntityPlayer) elb;
			double scale = type == ItemRenderType.EQUIPPED ? 0.6 : .8;
			GL11.glPushMatrix();
			{ //FIX: Added matrix state for transform.
				GL11.glScaled(scale, scale, scale);
				RenderUtils.drawEquippedItem(0.04, Resources.TEX_COIN_FRONT, Resources.TEX_COIN_BACK);
			}
			GL11.glPopMatrix();
		}
	}

}
