/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.event;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraftforge.common.config.Property;

/**
 * Fired whenever a specific property in AC config has been edited.
 * @author WeAthFolD
 */
public class ConfigModifyEvent extends Event {
    
    public final Property property;
    
    public ConfigModifyEvent(Property _prop) {
        property = _prop;
    }
    
}
