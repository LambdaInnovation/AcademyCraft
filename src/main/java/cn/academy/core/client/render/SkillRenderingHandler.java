/**
 * 
 */
package cn.academy.core.client.render;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ctrl.SkillState;
import cn.academy.api.ctrl.SkillStateManager;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.client.LIClientRegistry;
import cn.liutils.api.client.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public class SkillRenderingHandler {
	
	private static SkillRenderingHandler instance = new SkillRenderingHandler();
	
	public static void init() {
		LIClientRegistry.addPlayerRenderingHelper(new PRHSkillRender());
	}
	
	public static void renderThirdPerson(EntityLivingBase ent, ItemStack stack, ItemRenderType type) {
		if(type == ItemRenderType.EQUIPPED_FIRST_PERSON || !(ent instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) ent;
		Item item = stack.getItem();
		Block block = Block.getBlockFromItem(stack.getItem());
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			
			if(stack.getItemSpriteNumber() == 0 && item instanceof ItemBlock) { //block render routine
				GL11.glTranslated(0, 0, 1.5);
				GL11.glScalef(2F, 2F, 2F);
				GL11.glRotated(45, 1, 0, 1);
				GL11.glRotated(45, 0, 1, 0);
				GL11.glRotated(180, 1, 0, 0);
			} else if(item.isFull3D()) {
				GL11.glTranslated(0.1, 0.8, -.4);
				GL11.glScalef(.8F, .8F, .8F);
				GL11.glRotated(45, 0, 0, -1);
			} else {
				GL11.glTranslated(-.3, 1.2, -.6);
				GL11.glRotatef(90, 1, 0, 0);
				GL11.glScalef(1.5F, 1.5F, 1.5F);
			}
			
			//TODO Replace with skill renderer
			RenderUtils.loadTexture(ACClientProps.TEX_ARC_SHELL[0]);
			RenderUtils.drawCube(1, 1, 1, false);
			for(SkillState state : SkillStateManager.getStateForPlayer(player)) {
				//state.
			}
			
		} GL11.glPopMatrix();
	}
	
	/**
	 * 插入ItemRenderer的渲染路径
	 */
	public static void renderFirstPerson() {
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix(); {
			GL11.glTranslated(0.28, -0.55, -.9);
			GL11.glColor4d(1, 0.6, 0.6, 0.55);
			GL11.glScalef(0.66F, 0.66F, 0.66F);
			GL11.glRotatef(20, 1, 0, 0);
			
			//TODO Replace
			RenderUtils.loadTexture(ACClientProps.TEX_ARC_SHELL[0]);
			RenderUtils.drawCube(1, 1, 1, false);
			
		} GL11.glPopMatrix();
	}
	
	private static Vec3 vec(double x, double y, double z) {
		return Vec3.createVectorHelper(x, y, z);
	}
	
}
