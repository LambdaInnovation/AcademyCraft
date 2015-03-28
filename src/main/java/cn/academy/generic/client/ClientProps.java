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

import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.annoreg.mc.RegSubmoduleInit.Side;
import cn.liutils.util.render.LambdaFont;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@ForcePreloadTexture
@RegSubmoduleInit(side = Side.CLIENT_ONLY)
public class ClientProps {

    public static ResourceLocation
        TEX_GUI_PHONE_BACK = gui("phone_back"),
        TEX_GUI_APP_BG = gui("app_back");
    
    private static LambdaFont font;
    
    private static ResourceLocation[] fontLocation;
    static {
        fontLocation = new ResourceLocation[12];
        for(int i = 0; i < 12; ++i) {
            fontLocation[i] = res("fonts/yahei" + i + ".png");
        }
    }
    
    public static void init() {
        font = new LambdaFont("/assets/academy/fonts/yahei.lf", fontLocation);
    }
    
    public static LambdaFont font() {
        return font;
    }
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
    private static ResourceLocation gui(String loc) {
        return res("textures/guis/" + loc + ".png");
    }

}
