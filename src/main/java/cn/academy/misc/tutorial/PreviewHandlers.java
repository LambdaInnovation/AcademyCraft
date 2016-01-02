package cn.academy.misc.tutorial;

import cn.academy.core.client.Resources;
import cn.academy.misc.tutorial.client.RecipeHandler;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.vis.model.CompTransform;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import javax.vecmath.Vector2f;
import java.util.*;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

@Registrant
public final class PreviewHandlers {

    // This class used a SideOnly hack to make user be able to specify display on init without considering side only issue.

	private static Random random = new Random();

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

    public static IPreviewHandler[] recipes(Item item) {
        if (client()) return recipesImpl(item);
        return new IPreviewHandler[] { nothing };
    }

    public static IPreviewHandler[] recipes(Block block) {
        return recipes(Item.getItemFromBlock(block));
    }

    public static IPreviewHandler displayModel(
        String model,
        String textureName,
        CompTransform transform
    ) {
        if (client()) return displayModelImpl(model, textureName, transform);
        return nothing;
    }

    public static IPreviewHandler displayModel(String model, CompTransform transform) {
        return displayModel(model, model, transform);
    }

    public static IPreviewHandler displayIcon(
            String icon,
            Vector2f offset, float scale, Color color) {
        ResourceLocation icon_res = Resources.getTexture(icon);
        return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
            @Override
            public void draw() {
                RenderUtils.loadTexture(icon_res);
                color.bind();

                glDepthFunc(GL_ALWAYS);
                glPushMatrix();
                glTranslatef(offset.x, offset.y, 0);
                glScalef(scale, scale, 1);
                HudUtils.rect(-.5, -.5, 1, 1);
                glPopMatrix();
                glDepthFunc(GL_LEQUAL);
            }
        };
    }

    @SideOnly(Side.CLIENT)
    private static IPreviewHandler[] recipesImpl(Item item) {
        return Arrays.stream(RecipeHandler.instance.recipeOfItem(item))
                .map(PreviewHandlers::toPreview)
                .toArray(IPreviewHandler[]::new);
    }

    @SideOnly(Side.CLIENT)
    private static IPreviewHandler displayModelImpl(String mdl, String texture, CompTransform transform) {
        IModelCustom model = Resources.getModel(mdl);
        ResourceLocation res_tex = Resources.getTexture("models/" + texture);
        return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
            @Override
            public void draw() {
                glPushMatrix();
                glRotated((GameTimer.getAbsTime() / 50.0) % 360.0, 0, 1, 0);
                transform.doTransform();
                RenderUtils.loadTexture(res_tex);
                model.renderAll();
                glPopMatrix();
            }
        };
    }

    @SideOnly(Side.CLIENT)
    private static IPreviewHandler drawsBlockImpl(Block block, int metadata) {
        return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
            @Override
            public void draw() {
                final Minecraft mc = Minecraft.getMinecraft();
                final RenderBlocks renderer = RenderBlocks.getInstance();
                final Tessellator tes = Tessellator.instance;

                renderer.blockAccess = Minecraft.getMinecraft().theWorld;

                mc.theWorld.setBlock(0, 0, 0, block, metadata, 0x00);
                RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
                glCullFace(GL_BACK);
                glTranslated(0.15, 0.1, -1);
                glRotated((GameTimer.getAbsTime() / 80.0) % 360.0, 0, 1, 0);
                glScaled(.8, .8, .8);
                glTranslated(-.5, -.5, -.5);
                tes.startDrawingQuads();
                renderer.renderBlockAllFaces(block, 0, 0, 0);
                tes.draw();
                glCullFace(GL_FRONT);
            }
        };
    }

    @SideOnly(Side.CLIENT)
	private static IPreviewHandler drawsItemImpl(Item item, int metadata) {
		ItemStack stack = new ItemStack(item, 1, metadata);
		return new IPreviewHandler() {
            @SideOnly(Side.CLIENT)
			@Override
			public void draw() {
				glDepthFunc(GL_ALWAYS);
				RenderItem.renderInFrame = true;
				glTranslated(0.54, 0.5, 0);
                glScaled(-1/16.0, -1/16.0, 1);
                RenderUtils.loadTexture(TextureMap.locationItemsTexture);
				RenderUtils.renderItemInventory(stack);
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
