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
package cn.academy.knowledge;

import cn.academy.core.client.Resources;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 */
public class Knowledge {

    final String name;
    protected ResourceLocation icon;

    public Knowledge(String _name) {
        name = _name;

        icon = Resources.getTexture("knowledge/" + name.replace('.', '/'));
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    /**
     * Get the description text of this knowledge. Usually the way to acquire
     * this knowledge.
     */
    public String getDesc() {
        return StatCollector.translateToLocal("ac.knowledge." + name + ".desc");
    }

    /**
     * Get the name of this knowledge.
     */
    public String getName() {
        return StatCollector.translateToLocal("ac.knowledge." + name + ".name");
    }

    @Override
    public String toString() {
        return name;
    }

}
