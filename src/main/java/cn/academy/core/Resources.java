package cn.academy.core;

import cn.academy.core.client.ClientResources;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.util.client.font.IFont;
import cn.lambdalib2.util.client.font.IFont.FontOption;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Resources dispatcher.
 * @author WeathFolD
 */
public class Resources {
    
    // PUBLICLY USED RESOURCES
    // TODO Move textures that are not public to their own place
    
    public static ResourceLocation
        TEX_COIN_FRONT = res("textures/items/coin_front.png"),
        TEX_COIN_BACK = res("textures/items/coin_back.png");
    
    public static ResourceLocation
        TEX_EMPTY = getTexture("null");

    public static ResourceLocation
        TEX_GLOW_LINE = getTexture("effects/glow_line");
    
    public static ResourceLocation
        ARC_SMALL[] = getEffectSeq("arcs", 10);

    /**
     * @return A resources with path `loc` located in `academy` namespace.
     */
    public static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
    /**
     * Get the model instance of the given name. If the name is
     * first queried, will load that resource from the file system.
     * TODO need a new method for loading obj models
     */
    public static IModelCustom getModel(String mdlName) {
        return ClientResources.getModel(mdlName);
    }
    
    public static ResourceLocation getTexture(String loc) {
        return res("textures/" + loc + ".png");
    }

    /**
     * Get the resource location for the texture path specified, and preload it in the MC texture manager with generated
     *  mipmap.
     * @throws RuntimeException when texture IO failed
     */
    @SideOnly(Side.CLIENT)
    public static ResourceLocation preloadMipmapTexture(String loc) {
        return ClientResources.preloadMipmapTexture(loc);
    }

    @SideOnly(Side.CLIENT)
    public static ResourceLocation preloadTexture(String loc) {
        return ClientResources.preloadTexture(loc);
    }
    
    public static ResourceLocation getShader(String loc) {
        return res("shaders/" + loc);
    }

    public static ResourceLocation getGui(String loc) {
        return res("guis/" + loc + ".xml");
    }

    public static ResourceLocation[] getTextureSeq(String loc, int n) {
        ResourceLocation[] ret = new ResourceLocation[n];
        for(int i = 0; i < n; ++i)
            ret[i] = getTexture(loc + i);
        return ret;
    }
    
    public static ResourceLocation[] getEffectSeq(String effectName, int n) {
        ResourceLocation[] layers = new ResourceLocation[n];
        String baseName = "academy:textures/effects/" + effectName + "/";
        for(int i = 0; i < n; ++i) {
            layers[i] = new ResourceLocation(baseName + i + ".png");
        }
        return layers;
    }
    
    public static ResourceLocation[] getRayTextures(String name) {
        ResourceLocation r1 = new ResourceLocation("academy:textures/effects/" + name + "/blend_in.png");
        ResourceLocation r2 = new ResourceLocation("academy:textures/effects/" + name + "/tile.png");
        ResourceLocation r3 = new ResourceLocation("academy:textures/effects/" + name + "/blend_out.png");
        return new ResourceLocation[] { r1, r2, r3 };
    }

    @SideOnly(Side.CLIENT)
    public static IFont font() {
        return ClientResources.font();
    }

    @SideOnly(Side.CLIENT)
    public static IFont fontBold() {
        return ClientResources.fontBold();
    }

    @SideOnly(Side.CLIENT)
    public static IFont fontItalic() {
        return ClientResources.fontItalic();
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @SideOnly(Side.CLIENT)
    public static TextBox newTextBox() {
        return ClientResources.newTextBox();
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @SideOnly(Side.CLIENT)
    public static TextBox newTextBox(FontOption option) {
        return ClientResources.newTextBox(option);
    }
    
}
