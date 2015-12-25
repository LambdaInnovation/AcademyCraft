package cn.academy.misc.tutorial;

import cn.academy.misc.tutorial.client.RecipeHandler;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.util.client.RenderUtils;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
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

import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

@Registrant
public final class PreviewHandlers {

	private static Random random = new Random();

    @SideOnly(Side.CLIENT)
	private static RenderItem renderItem;

    @SideOnly(Side.CLIENT)
    @RegInitCallback
	public static void init() {
        renderItem = new RenderItem();
		renderItem.setRenderManager(RenderManager.instance);
	}

	public static final IPreviewHandler nothing = new IPreviewHandler() {};

    public static IPreviewHandler drawsBlock(Block block) {
        return drawsBlock(block, 0);
    }

    public static IPreviewHandler drawsBlock(Block block, int meta) {
        if (client()) return drawsBlockImpl(block, meta);
        return nothing;
    }

	public static IPreviewHandler drawsItem(Item item) {
		return drawsItem(item, 0);
	}

    public static IPreviewHandler drawsItem(Item item, int metadata) {
        if (client()) return drawsItemImpl(item, metadata);
        return nothing;
    }

    public static IPreviewHandler[] plainDisplay(Item item) {
        if (client()) return plainDisplayImpl(item);
        return new IPreviewHandler[] { nothing };
    }

    public static IPreviewHandler[] plainDisplay(Block block) {
        return plainDisplay(Item.getItemFromBlock(block));
    }

    @SideOnly(Side.CLIENT)
    private static IPreviewHandler[] plainDisplayImpl(Item item) {
        List<IPreviewHandler> res = new ArrayList<>();
        res.add(drawsItem(item));
        res.addAll(Arrays.stream(RecipeHandler.instance.recipeOfItem(item))
                .map(PreviewHandlers::toPreview)
                .collect(Collectors.toList()));
        return res.toArray(new IPreviewHandler[res.size()]);
    }

    @SideOnly(Side.CLIENT)
    private static IPreviewHandler drawsBlockImpl(Block block, int metadata) {
        Minecraft mc = Minecraft.getMinecraft();
        return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
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

    @SideOnly(Side.CLIENT)
	private static IPreviewHandler drawsItemImpl(Item item, int metadata) {
		Minecraft mc = Minecraft.getMinecraft();
		ItemStack stack = new ItemStack(item, 1, metadata);
		EntityItem fake = new EntityItem(
				Minecraft.getMinecraft().theWorld,
				0, 0, 0, stack);
		return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
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

    @SideOnly(Side.CLIENT)
	private static Collection<IPreviewHandler> recipesOfStackImpl(ItemStack stack) {
        return Arrays.stream(RecipeHandler.instance.recipeOfStack(stack)).map(w -> new IPreviewHandler() {
            @Override
            public Widget getDelegateWidget() {
                return w;
            }
        }).collect(Collectors.toList());
	}

    private static IPreviewHandler toPreview(Widget delegate) {
        return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
            @Override
            public Widget getDelegateWidget() {
                return delegate;
            }
        };
    }

    private static boolean client() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    private PreviewHandlers() {}
}
