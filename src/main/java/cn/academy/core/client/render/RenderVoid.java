/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import cn.liutils.api.client.util.RenderUtils;

public class RenderVoid implements IItemRenderer {
	
	protected static ModelBiped model = new ModelBiped();
	private static final ResourceLocation steveTexture = new ResourceLocation("minecraft:textures/entity/steve.png");

	public RenderVoid() {}

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
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		//Just render a hand
		if(type != ItemRenderType.EQUIPPED_FIRST_PERSON) return;
		if(!(data[1] instanceof EntityPlayer))
			return;
		EntityPlayer player = (EntityPlayer) data[1];
		renderHand(player);
	}
	
	public static final void renderHand(EntityPlayer player) {
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glPushMatrix();

		RenderUtils.renderEnchantGlint_Equip();
		RenderUtils.loadTexture(steveTexture);
		GL11.glRotated(-23.75, 0.0F, 0.0F, 1.0F);
		GL11.glRotated(21.914, 0.0F, 1.0F, 0.0F);
		GL11.glRotated(32.75, 1.0F, 0.0F, 0.0F);
		GL11.glTranslatef(.758F, -.072F, -.402F);
		GL11.glColor3f(1.0F, 1.0F, 1.0F);

		model.onGround = 0.0F;
		model.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, player);
		model.bipedRightArm.render(0.0625F);

		
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

}
