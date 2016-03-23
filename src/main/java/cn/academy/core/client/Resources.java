/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.ForcePreloadTexture;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.client.font.Fonts;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.TrueTypeFont;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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
        checkInit();
        return font;
    }
    public static IFont fontBold() {
        checkInit();
        return fontBold;
    }
    public static IFont fontItalic() {
        checkInit();
        return fontItalic;
    }

    private static void checkInit() {
        if (!init) {
            init = true;

            font = TrueTypeFont.withFallback2(Font.PLAIN, 32,
                    new String[] { "Microsoft YaHei", "Adobe Heiti Std R", "STHeiti", "Consolas", "Monospace", "Arial" });
            fontBold = new TrueTypeFont(font.font().deriveFont(Font.BOLD));
            fontItalic = new TrueTypeFont(font.font().deriveFont(Font.ITALIC));
        }
    }

    @SideOnly(Side.CLIENT)
    @RegInitCallback
    public static void __init() {
        checkInit();

        Fonts.register("AC_Normal", font());
        Fonts.register("AC_Bold", fontBold());
        Fonts.register("AC_Italic", fontItalic());
    }
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
}
