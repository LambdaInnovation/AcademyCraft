package cn.academy.terminal.app.settings;

import org.lwjgl.input.Keyboard;

import cn.academy.core.client.Resources;
import cn.academy.core.event.ConfigModifyEvent;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.event.GainFocusEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.cgui.gui.event.KeyEvent;
import cn.lambdalib.cgui.gui.event.MouseDownEvent;
import cn.lambdalib.cgui.gui.event.global.GlobalMouseEvent;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.KeyManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class PropertyElements {
	
	public static IPropertyElement CHECKBOX = new IPropertyElement() {
		
		final ResourceLocation 
			CHECK_TRUE = Resources.getTexture("guis/check_true"),
			CHECK_FALSE = Resources.getTexture("guis/check_false");

		@Override
		public Widget getWidget(UIProperty prop) {
			Configuration cfg = getConfig();
			Property p = cfg.get(prop.category, prop.id, (boolean) prop.defValue);
			
			Widget ret = SettingsUI.loaded.getWidget("t_checkbox").copy();
			TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
			
			Widget check = ret.getWidget("box");
			DrawTexture.get(check).setTex(p.getBoolean() ? CHECK_TRUE : CHECK_FALSE);
			
			check.listen(MouseDownEvent.class, (w, e) -> {
				boolean b = !p.getBoolean();
				p.set(b);
				DrawTexture.get(check).setTex(b ? CHECK_TRUE : CHECK_FALSE);
				MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(p));
			});
			
			return ret;
		}
		
	},
	
	KEY = new IPropertyElement() {
		
		@Override
		public Widget getWidget(UIProperty prop) {
			Configuration cfg = getConfig();
			Property p = cfg.get(prop.category, prop.id, (int) prop.defValue);
			
			Widget ret = SettingsUI.loaded.getWidget("t_key").copy();
			TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
			
			Widget key = ret.getWidget("key");
			key.addComponent(new EditKey(p));
			
			return ret;
		}
		
	};
	
	private static class EditKey extends Component {
		
		static final Color 
			CRL_NORMAL = new Color().setColor4i(200, 200, 200, 200),
			CRL_EDIT = new Color().setColor4i(251, 133, 37, 200);
		
		IGuiEventHandler<GlobalMouseEvent> gMouseHandler;
		
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
			textBox.color = CRL_EDIT;
			
			widget.getGui().eventBus.listen(GlobalMouseEvent.class, 
			gMouseHandler = (w, event) -> {
				endEditing(event.key - 100);
			});
		}
		
		private void endEditing(int key) {
			editing = false;
			textBox.color = CRL_NORMAL;
			widget.getGui().removeFocus();
			
			if(key == Keyboard.KEY_ESCAPE) {
				;
			} else {
				prop.set(key);
			}
			
			updateKeyName();
			widget.getGui().eventBus.unlisten(GlobalMouseEvent.class, gMouseHandler);
			MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(prop));
		}
		
	}
	
}
