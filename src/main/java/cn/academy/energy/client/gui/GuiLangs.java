/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.gui;

import net.minecraft.util.StatCollector;

/**
 * @author WeAthFolD
 *
 */
public class GuiLangs {

    public static final String loadingUppercase() {
        return local("loading_uppercase");
    }
    
    public static final String loadingLowercase() {
        return local("loading_lowercase");
    }
    
    private static final String local(String name) {
        return StatCollector.translateToLocal("ac.gui." + name + ".desc");
    }
    
}
