package cn.academy.misc.tutorial;

import cn.academy.core.client.render.block.RenderDynamicBlock;
import cn.academy.misc.tutorial.client.RecipeHandler;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.GameTimer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;

import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

// TODO Currently doesn't play well with server
public final class PreviewHandlers {

	private static Random random = new Random();

	private static RenderItem renderItem = new RenderItem();

	static {
		renderItem.setRenderManager(RenderManager.instance);
	}

	private PreviewHandlers() {}

	public static final IPreviewHandler nothing = new IPreviewHandler() {};

	public static IPreviewHandler drawsBlock(Block block) {
		return drawsBlock(block, 0);
	}

	public static IPreviewHandler drawsBlock(Block block, int metadata) {
		Minecraft mc = Minecraft.getMinecraft();
		return new IPreviewHandler() {
			@Override
			public void draw() {
				RenderBlocks renderer = RenderBlocks.getInstance();
				renderer.blockAccess = mc.theWorld;
				Tessellator tes = Tessellator.instance;
				mc.theWorld.setBlock(0, 0, 0, block, metadata, 0x00);
				RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
				glRotated(180, 0, 1, 0);
				glTranslated(-.5, -.5, -.5);
				tes.startDrawingQuads();
				renderer.renderBlockAllFaces(block, 0, 0, 0);
				tes.draw();
			}
		};
	}

	public static IPreviewHandler drawsItem(Item item) {
		return drawsItem(item, 0);
	}

	public static IPreviewHandler drawsItem(Item item, int metadata) {
		Minecraft mc = Minecraft.getMinecraft();
		ItemStack stack = new ItemStack(item, 1, metadata);
		EntityItem fake = new EntityItem(
				Minecraft.getMinecraft().theWorld,
				0, 0, 0, stack);
		return new IPreviewHandler() {
			@Override
			public void draw() {
				glDepthFunc(GL_ALWAYS);
				RenderItem.renderInFrame = true;
				glTranslated(0, -0.3, 0);
				renderItem.doRender(fake, 0, 0, 0, 0, 0);
				glDepthFunc(GL_LEQUAL);
			}
		};
	}

	public static IPreviewHandler recipesOfStack(ItemStack stack) {
		return new IPreviewHandler() {
			@Override
			public Widget getDelegateWidget() {
				return RecipeHandler.instance.recipeOfStack(stack);
			}
		};
	}

}
