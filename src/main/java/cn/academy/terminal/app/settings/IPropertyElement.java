package cn.academy.terminal.app.settings;

import cn.academy.AcademyCraft;
import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public abstract class IPropertyElement<T extends UIProperty> {
    
    public abstract Widget getWidget(T prop);
    
    public Configuration getConfig() {
        return AcademyCraft.config;
    }
    
}