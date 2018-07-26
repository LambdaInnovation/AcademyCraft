package cn.academy.terminal.app.settings;

import cn.academy.core.AcademyCraft;
import cn.lambdalib2.cgui.Widget;
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