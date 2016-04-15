/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client;

import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.ForcePreloadTexture;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegPreInitCallback;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.util.client.font.Fonts;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.client.font.TrueTypeFont;
import cn.lambdalib.util.generic.RegistryUtils;
import com.google.common.base.Throwables;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.common.config.Configuration;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * The core resource utils of AC.
 * @author WeathFolD
 */
@Registrant
@ForcePreloadTexture
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
    
    private static Map<String, IModelCustom> cachedModels = new HashMap<>();
    
    /**
     * Get the model instance of the given name. If the name is
     * first queried, will load that resource from the file system.
     */
    public static IModelCustom getModel(String mdlName) {
        IModelCustom ret = cachedModels.get(mdlName);
        if(ret != null)
            return ret;
        ret = AdvancedModelLoader.loadModel(res("models/" + mdlName + ".obj"));
        cachedModels.put(mdlName, ret);
        return ret;
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
        ResourceLocation ret = getTexture(loc);

        try {
            BufferedImage buffer = ImageIO.read(RegistryUtils.getResourceStream(ret));

            // Note: Here we should actually implement ITextureObject,
            // but that causes problems when running with SMC because SMC adds an abstract method in the base
            // interface (getMultiTexID) and we have no way to implement it easily.
            // However it is automatically implemented in AbstractTexture.
            // (Go to hell shadersmod!)

            Minecraft.getMinecraft().getTextureManager().loadTexture(ret, new AbstractTexture() {

                final int textureID = glGenTextures();

                {
                    int width = buffer.getWidth(), height = buffer.getHeight();
                    int[] data = new int[width * height];
                    buffer.getRGB(0, 0, width, height, data, 0, width);
                    IntBuffer buffer1 = BufferUtils.createIntBuffer(data.length);
                    buffer1.put(data).flip();

                    glBindTexture(GL_TEXTURE_2D, textureID);
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer1);

                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
                    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

                    GL30.glGenerateMipmap(GL_TEXTURE_2D);

                    glBindTexture(GL_TEXTURE_2D, 0);
                }

                @Override
                public void loadTexture(IResourceManager man) throws IOException {}

                @Override
                public int getGlTextureId() {
                    return textureID;
                }
            });
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }

        return ret;
    }

    @SideOnly(Side.CLIENT)
    public static ResourceLocation preloadTexture(String loc) {
        ResourceLocation ret = getTexture(loc);
        Minecraft.getMinecraft().getTextureManager().loadTexture(ret, new SimpleTexture(ret));
        return ret;
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

    private static boolean init = false;
    private static TrueTypeFont font, fontBold, fontItalic;
    public static IFont font() {
        checkFontInit();
        return font;
    }
    public static IFont fontBold() {
        checkFontInit();
        return fontBold;
    }
    public static IFont fontItalic() {
        checkFontInit();
        return fontItalic;
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @SideOnly(Side.CLIENT)
    public static TextBox newTextBox() {
        TextBox ret = new TextBox();
        ret.font = font();
        return ret;
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @SideOnly(Side.CLIENT)
    public static TextBox newTextBox(FontOption option) {
        TextBox ret = new TextBox(option);
        ret.font = font();
        return ret;
    }

    private static void checkFontInit() {
        if (!init) {
            init = true;

            // TODO: Add this to settings page, though it will require restart to take effect

            Configuration config = AcademyCraft.config;
            String userSpecified = config.getString("font", "gui", "Microsoft YaHei",
                    "The font to be used. If not found in the system, default fonts will be used.");

            font = TrueTypeFont.withFallback2(Font.PLAIN, 32,
                    new String[] {
                        userSpecified,
                        "微软雅黑",
                        "Microsoft YaHei",
                        "SimHei",
                        "Adobe Heiti Std R"
                    });
            fontBold = new TrueTypeFont(font.font().deriveFont(Font.BOLD));
            fontItalic = new TrueTypeFont(font.font().deriveFont(Font.ITALIC));
        }
    }

    @SideOnly(Side.CLIENT)
    @RegPreInitCallback
    public static void __preInit() {
        checkFontInit();
        // TODO: Disaster if any ui initializes before preInit...

        Fonts.register("AC_Normal", font());
        Fonts.register("AC_Bold", fontBold());
        Fonts.register("AC_Italic", fontItalic());
    }
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
}
