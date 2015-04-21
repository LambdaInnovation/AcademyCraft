/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.generic.client;

import net.minecraft.util.ResourceLocation;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.annoreg.mc.RegSubmoduleInit.Side;
import cn.liutils.util.render.Font;
import cn.liutils.util.render.LambdaFont;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@ForcePreloadTexture
public class ClientProps {

    public static ResourceLocation
        TEX_PHONE_BACK = phone("phone_back"),
        TEX_PHONE_APP_BG = phone("app_back"),
        TEX_PHONE_SYNC_MASK = phone("sync_mask"),
        TEX_PHONE_SYNC = phone("sync"),
        TEX_PHONE_HINT_ML = phone("hint_mouse_left"),
        TEX_PHONE_HINT_MR = phone("hint_mouse_right"),
        TEX_PHONE_ARROW = phone("arrow");
    
    public static ResourceLocation 
        TEX_GUI_NODE = gui("node"),
        TEX_GUI_NODE_LIST = gui("node_list");
    
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
