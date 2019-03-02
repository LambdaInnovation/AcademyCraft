package cn.academy.terminal.app.settings;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class UIProperty {

    public static class Config extends UIProperty {
        public final String category;
        public final Object defValue;

        public Config(IPropertyElement _element, String _category,
                      String _id, Object _defValue, boolean _singlePlayer) {
            super(_element, _id, _singlePlayer);
            category = _category;
            defValue = _defValue;
        }
    }

    public static class Callback extends UIProperty {

        Runnable action;

        public Callback(IPropertyElement _element, String _id, Runnable _action, boolean _singlePlayer) {
            super(_element, _id, _singlePlayer);
            action = _action;
        }
    }

    public final IPropertyElement element;
    public final String id;
    public final boolean singlePlayer;
    
    public UIProperty(IPropertyElement _element, String _id, boolean _singlePlayer) {
        element = _element;
        id = _id;
        singlePlayer = _singlePlayer;
    }

    @SideOnly(Side.CLIENT)
    public String getDisplayID() {
        return I18n.format("ac.settings.prop." + id);
    }
    
}