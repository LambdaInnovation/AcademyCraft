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

/**
 * @author WeathFolD
 *
 */
public class ClientProps {

    public static ResourceLocation
        TEX_GUI_PHONE_BACK = gui("phone_back.png");
    
    private static ResourceLocation res(String loc) {
        return new ResourceLocation("academy:" + loc);
    }
    
    private static ResourceLocation gui(String loc) {
        return res("textures/guis/" + loc);
    }

}
