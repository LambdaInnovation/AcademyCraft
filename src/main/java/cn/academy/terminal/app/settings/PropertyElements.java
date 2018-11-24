package cn.academy.terminal.app.settings;

import cn.academy.Resources;
import cn.academy.event.ConfigModifyEvent;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.*;
import cn.lambdalib2.input.KeyManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Color;

@SideOnly(Side.CLIENT)
public class PropertyElements {
    
    public static IPropertyElement CHECKBOX = new IPropertyElement<UIProperty.Config>() {
        
        final ResourceLocation 
            CHECK_TRUE = Resources.getTexture("guis/check_true"),
            CHECK_FALSE = Resources.getTexture("guis/check_false");

        @Override
        public Widget getWidget(UIProperty.Config prop) {
            Configuration cfg = getConfig();
            Property p = cfg.get(prop.category, prop.id, (boolean) prop.defValue);
            
            Widget ret = SettingsUI.document.getWidget("t_checkbox").copy();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
            
            Widget check = ret.getWidget("box");
            DrawTexture.get(check).setTex(p.getBoolean() ? CHECK_TRUE : CHECK_FALSE);
            
            check.listen(LeftClickEvent.class, (w, e) -> {
                boolean b = !p.getBoolean();
                p.set(b);
                DrawTexture.get(check).setTex(b ? CHECK_TRUE : CHECK_FALSE);
                MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(p));
            });
            
            return ret;
        }
        
    },
    
    KEY = new IPropertyElement<UIProperty.Config>() {
        
        @Override
        public Widget getWidget(UIProperty.Config prop) {
            Configuration cfg = getConfig();
            Property p = cfg.get(prop.category, prop.id, (int) prop.defValue);
            
            Widget ret = SettingsUI.document.getWidget("t_key").copy();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
            
            Widget key = ret.getWidget("key");
            key.addComponent(new EditKey(p));
            
            return ret;
        }
        
    },

    CALLBACK = new IPropertyElement<UIProperty.Callback>() {
        @Override
        public Widget getWidget(UIProperty.Callback prop) {
            Widget ret = SettingsUI.document.getWidget("t_callback").copy();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
            ret.getWidget("button").listen(LeftClickEvent.class, (w, e) -> prop.action.run());
            return ret;
        }
    };
    
    private static class EditKey extends Component {
        
        static final Color
            CRL_NORMAL = new Color(200, 200, 200, 200),
            CRL_EDIT = new Color(251, 133, 37, 200);
        
        IGuiEventHandler<MouseClickEvent> gMouseHandler;
        
        final Property prop;

        public boolean editing;
        
        TextBox textBox;
        
        public EditKey(Property _prop) {
            super("EditKey");
            
            prop = _prop;
            
            listen(KeyEvent.class, (w, event) -> 
            {
                if(editing) {
                    endEditing(event.keyCode);
                }
            });
            
            listen(GainFocusEvent.class, (w, e) ->
            {
                startEditing();
            });
        }
        
        @Override
        public void onAdded() {
            super.onAdded();

            textBox = TextBox.get(widget);
            widget.transform.doesListenKey = true;
            updateKeyName();
        }
        
        private void updateKeyName() {
            textBox.setContent(KeyManager.getKeyName(prop.getInt()));
        }
        
        private void startEditing() {
            editing = true;
            textBox.setContent("PRESS");
            textBox.option.color = CRL_EDIT;
            
            widget.getGui().listen(MouseClickEvent.class,
            gMouseHandler = (w, event) -> {
                endEditing(event.button - 100);
            });
        }
        
        private void endEditing(int key) {
            editing = false;
            textBox.option.color = CRL_NORMAL;
            widget.getGui().removeFocus();
            
            if(key == Keyboard.KEY_ESCAPE) {
                ;
            } else {
                prop.set(key);
            }
            
            updateKeyName();
            widget.getGui().unlisten(MouseClickEvent.class, gMouseHandler);
            MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(prop));
        }
        
    }
    
}