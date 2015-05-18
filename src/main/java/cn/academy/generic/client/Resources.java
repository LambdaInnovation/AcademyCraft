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
package cn.academy.generic.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.liutils.render.material.Material;
import cn.liutils.render.material.SimpleMaterial;
import cn.liutils.util.render.Font;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@ForcePreloadTexture
public class Resources {
    
    public static ResourceLocation 
        TEX_GUI_NODE = gui("node"),
        TEX_GUI_NODE_LIST = gui("node_list");
    
    private static Map<String, IModelCustom> createdModels = new HashMap();
    
    /**
     * Get the model instance of the given name. If the name is
     * first queried, will load that resource from the file system.
     */
    public static IModelCustom getModel(String mdlName) {
    	IModelCustom ret = createdModels.get(mdlName);
    	if(ret != null)
    		return ret;
    	ret = AdvancedModelLoader.loadModel(res("models/" + mdlName + ".obj"));
    	createdModels.put(mdlName, ret);
    	return ret;
    }
    
    public static ResourceLocation getTexture(String loc) {
    	return res("textures/" + loc + ".png");
    }
    
    public static ResourceLocation[] getEffectSeq(String effectName, int n) {
    	ResourceLocation[] layers = new ResourceLocation[n];
		String baseName = "academy:textures/effects/" + effectName + "/";
		for(int i = 0; i < n; ++i) {
			layers[i] = new ResourceLocation(baseName + i + ".png");
		}
		return layers;
    }
    
    public static Material[] getRayTextures(String name) {
    	SimpleMaterial ms1 = new SimpleMaterial(new ResourceLocation("academy:textures/effects/" + name + "/blend_in.png")).setIgnoreLight();
    	SimpleMaterial ms2 = new SimpleMaterial(new ResourceLocation("academy:textures/effects/" + name + "/tile.png")).setIgnoreLight();
    	SimpleMaterial ms3 = new SimpleMaterial(new ResourceLocation("academy:textures/effects/" + name + "/blend_out.png")).setIgnoreLight();
    	return new Material[] { ms1, ms2, ms3 };
    }
    
    public static Font font() {
        return Font.font;
    }
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
    private static ResourceLocation gui(String loc) {
        return res("textures/guis/" + loc + ".png");
    }

    private static ResourceLocation phone(String gloc) {
        return res("textures/phone/" + gloc + ".png");
    }
    
}
