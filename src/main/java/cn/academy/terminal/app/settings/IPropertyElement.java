/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.app.settings;

import cn.academy.core.AcademyCraft;
import cn.lambdalib2.cgui.gui.Widget;
import net.minecraftforge.common.config.Configuration;

/**
 * @author WeAthFolD
 */
public abstract class IPropertyElement<T extends UIProperty> {
    
    public abstract Widget getWidget(T prop);
    
    public Configuration getConfig() {
        return AcademyCraft.config;
    }
    
}
