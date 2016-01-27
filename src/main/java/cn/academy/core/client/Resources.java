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
package cn.academy.core.client;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.ForcePreloadTexture;
import cn.lambdalib.annoreg.mc.RegInitCallback;
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
        ARC_SMALL[] = getEffectSeq("arcs", 10);
    
    private static Map<String, IModelCustom> cachedModels = new HashMap();
    
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
    @RegInitCallback
    public static void init() {
        font = TrueTypeFont.withFallback2(Font.PLAIN, 32,
                new String[] { "微软雅黑", "黑体", "STHeiti", "Consolas", "Monospace", "Arial" });
        fontBold = new TrueTypeFont(font.font().deriveFont(Font.BOLD));
        fontItalic = new TrueTypeFont(font.font().deriveFont(Font.ITALIC));
    }

    private static TrueTypeFont font, fontBold, fontItalic;
    public static IFont font() {
        return font;
    }
    public static IFont fontBold() {
        return fontBold;
    }
    public static IFont fontItalic() {
        return fontItalic;
    }
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
}
