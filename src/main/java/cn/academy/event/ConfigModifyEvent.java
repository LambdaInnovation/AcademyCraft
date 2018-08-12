package cn.academy.event;

import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.eventhandler.Event;

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