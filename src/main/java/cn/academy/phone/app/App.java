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
package cn.academy.phone.app;

import java.lang.reflect.Constructor;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.phone.gui.GuiPhone;
import cn.liutils.api.gui.Widget;

/**
 * The provided gui class MUST preserve the (GuiPhone) constructor.
 * @author WeathFolD
 */
public class App {
    
    int id;
    final String name;
    final ResourceLocation icon;
    final int level;
    Constructor<? extends Widget> wigCtor;

    public App(String _name, Class<? extends Widget> _guiClazz, int _level) {
        name = _name;
        icon = new ResourceLocation("academy:textures/apps/" + name + ".png");
        level = _level;
        try {
            wigCtor = _guiClazz.getConstructor(GuiPhone.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getDisplayName() {
        return StatCollector.translateToLocal("ac.app." + name);
    }
    
    public String getAppName() {
        return name;
    }
    
    public int getID() {
        return id;
    }
    
    public Widget createWidget(GuiPhone gui) {
        try {
            return wigCtor.newInstance(gui);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
