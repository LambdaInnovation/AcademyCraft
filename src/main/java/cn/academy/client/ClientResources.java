package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.TrueTypeFont;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.render.obj.ObjParser;
import cn.lambdalib2.util.ResourceUtils;
import com.google.common.base.Throwables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;

/**
 * A delegation for client resources loading. Should not refer to explicitly.
 * @see Resources
 */
@SideOnly(Side.CLIENT)
public class ClientResources {

    private static boolean fontsInit = false;
    private static TrueTypeFont font, fontBold, fontItalic;

    private static final Map<ResourceLocation, ObjLegacyRender> cachedModels = new HashMap<>();

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

    public static ResourceLocation preloadMipmapTexture(String loc) {
        TextureManager texManager = Minecraft.getMinecraft().getTextureManager();

        ResourceLocation ret = Resources.getTexture(loc);

        ITextureObject loadedTexture = texManager.getTexture(ret);
        if (loadedTexture == null) {
            try {
                BufferedImage buffer = ImageIO.read(ResourceUtils.getResourceStream(ret));

                // Note: Here we should actually implement ITextureObject,
                // but that causes problems when running with SMC because SMC adds an abstract method in the base
                // interface (getMultiTexID) and we have no way to implement it easily.
                // However it is automatically implemented in AbstractTexture.

                texManager.loadTexture(ret, new AbstractTexture() {

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
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
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
        }

        return ret;
    }

    public static ResourceLocation preloadTexture(String loc) {
        ResourceLocation ret = Resources.getTexture(loc);

        TextureManager texManager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject loadedTexture = texManager.getTexture(ret);
        if (loadedTexture == null) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(ret, new SimpleTexture(ret));
        }

        return ret;
    }

    public static TextBox newTextBox() {
        TextBox ret = new TextBox();
        ret.font = font();
        return ret;
    }

    public static TextBox newTextBox(IFont.FontOption option) {
        TextBox ret = new TextBox(option);
        ret.font = font();
        return ret;
    }

    public static ObjLegacyRender getModel(String mdlName) {
        return cachedModels.computeIfAbsent(
            new ResourceLocation("academy", "models/" + mdlName + ".obj"), 
            (loc) -> new ObjLegacyRender(ObjParser.parse(loc))
        );
    }

    private static void checkFontInit() {
        if (!fontsInit) {
            fontsInit = true;

            Configuration config = AcademyCraft.config;
            String userSpecified = config.getString("font", "gui", "Microsoft YaHei",
                    "The font to be used. If not found in the system, default fonts will be used.");

            font = TrueTypeFont.withFallback(
                Font.PLAIN, 24, userSpecified,
                "微软雅黑",
                "Microsoft YaHei",
                "SimHei",
                "Adobe Heiti Std R"
            );
            fontBold = new TrueTypeFont(font.font.deriveFont(Font.BOLD));
            fontItalic = new TrueTypeFont(font.font.deriveFont(Font.ITALIC));
        }
    }

    @StateEventCallback
    private static void __preInit(FMLPreInitializationEvent event) {
        checkFontInit();

        Fonts.register("AC_Normal", font());
        Fonts.register("AC_Bold", fontBold());
        Fonts.register("AC_Italic", fontItalic());
    }
}